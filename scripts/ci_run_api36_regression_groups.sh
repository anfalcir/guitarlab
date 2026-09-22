#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TOTAL_GROUPS=5
GROUP_TIMEOUT_SECONDS="${API36_GROUP_TIMEOUT_SECONDS:-480}"
HEARTBEAT_SECONDS="${API36_HEARTBEAT_SECONDS:-60}"
DIAG_ROOT="$ROOT/ci-diagnostics/api36-groups"
mkdir -p "$DIAG_ROOT"

GROUP1_CLASSES=(
  CohesionResponsiveAccessibilityInstrumentedTest
  GuitarLabLifecycleInstrumentedTest
  HomeProjectLibraryInstrumentedTest
  ProjectDeleteConfirmationInstrumentedTest
  UnifiedProjectShellInstrumentedTest
)
GROUP2_CLASSES=(
  AllTracksLevelDialogInstrumentedTest
  AutoSectionsSlotInstrumentedTest
  MixerDockInstrumentedTest
  PhysicalEditingHardeningInstrumentedTest
  PracticeWorkflowInstrumentedTest
  RecordingCountdownOverlayInstrumentedTest
  TransportBarInstrumentedTest
)
GROUP3_CLASSES=(
  AudioImportCodecMatrixInstrumentedTest
  ExternalControlSettingsInstrumentedTest
  SettingsCalibrationModalInstrumentedTest
  SettingsVisualHierarchyInstrumentedTest
  StudioUiPreferencesStoreInstrumentedTest
)
GROUP4_CLASSES=(
  ActivityNotificationDeepLinkInstrumentedTest
  AndroidMasterAudioEncoderInstrumentedTest
  BackupScreenInstrumentedTest
  ProjectExportMediaStoreInstrumentedTest
  StudyExportInstrumentedTest
)
GROUP5_CLASSES=(
  TargetTabletGeometryInstrumentedTest
)

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  SUMMARY_FILE="$GITHUB_STEP_SUMMARY"
else
  SUMMARY_FILE="$DIAG_ROOT/summary.md"
  : > "$SUMMARY_FILE"
fi

{
  echo "### API 36 — regressão instrumentada"
  echo
  echo "Timeout por grupo: ${GROUP_TIMEOUT_SECONDS}s · heartbeat: ${HEARTBEAT_SECONDS}s"
  echo
  echo "| Grupo | Estado | Testes observados | Duração |"
  echo "|---|---:|---:|---:|"
} >> "$SUMMARY_FILE"

format_duration() {
  local seconds="$1"
  printf '%dm%02ds' "$((seconds / 60))" "$((seconds % 60))"
}

class_filter() {
  local out=""
  local cls
  for cls in "$@"; do
    [[ -n "$out" ]] && out+=","
    out+="studio.guitarlab.app.$cls"
  done
  printf '%s' "$out"
}

verify_test_coverage() {
  local expected actual diff
  expected="$(
    printf '%s\n' \
      "${GROUP1_CLASSES[@]}" \
      "${GROUP2_CLASSES[@]}" \
      "${GROUP3_CLASSES[@]}" \
      "${GROUP4_CLASSES[@]}" \
      "${GROUP5_CLASSES[@]}" \
      | sort -u
  )"
  actual="$(
    find app/src/androidTest/java/studio/guitarlab/app \
      -maxdepth 1 -type f -name '*InstrumentedTest.kt' -printf '%f\n' \
      | sed 's/\.kt$//' \
      | sort -u
  )"
  diff="$(comm -3 <(printf '%s\n' "$expected") <(printf '%s\n' "$actual") || true)"

  if [[ -n "$diff" ]]; then
    echo "::error title=API36 test coverage::Classe instrumentada sem grupo, ou grupo apontando para classe ausente:"
    printf '%s\n' "$diff"
    return 1
  fi

  local count
  count="$(printf '%s\n' "$actual" | sed '/^$/d' | wc -l | tr -d ' ')"
  echo "::notice title=API36 test coverage::Cobertura fail-closed validada: ${count} classes classificadas"
}

copy_group_evidence() {
  local slug="$1"
  local dest="$DIAG_ROOT/$slug"
  mkdir -p "$dest"

  if [[ -d app/build/outputs/androidTest-results/connected ]]; then
    rm -rf "$dest/results"
    cp -R app/build/outputs/androidTest-results/connected "$dest/results"
  fi
  if [[ -d app/build/reports/androidTests/connected ]]; then
    rm -rf "$dest/reports"
    cp -R app/build/reports/androidTests/connected "$dest/reports"
  fi
}

count_testcases() {
  local dir="$1"
  if [[ ! -d "$dir" ]]; then
    echo "0"
    return
  fi

  local count=0
  local file matches
  while IFS= read -r -d '' file; do
    matches="$(grep -c '<testcase ' "$file" 2>/dev/null || true)"
    count="$((count + ${matches:-0}))"
  done < <(find "$dir" -type f -name '*.xml' -print0)

  echo "$count"
}

collect_visual_evidence() {
  local slug="$1"
  local dest="$DIAG_ROOT/$slug/screenshots"
  local remote name count=0
  local remotes=""

  mkdir -p "$dest"
  remotes="$(
    adb shell run-as studio.guitarlab.app sh -c \
      'find files/ci-screenshots -maxdepth 1 -type f -name "*.png" -print' \
      2>/dev/null | tr -d '\r' || true
  )"

  while IFS= read -r remote; do
    [[ -n "$remote" ]] || continue
    name="$(basename "$remote")"
    if adb exec-out run-as studio.guitarlab.app cat "$remote" > "$dest/$name" 2>/dev/null &&
       [[ -s "$dest/$name" ]]; then
      count="$((count + 1))"
    else
      rm -f "$dest/$name"
    fi
  done <<< "$remotes"

  adb shell run-as studio.guitarlab.app rm -rf files/ci-screenshots >/dev/null 2>&1 || true

  if [[ "$count" -gt 0 ]]; then
    echo "::notice title=C8 screenshots::${count} screenshot(s) coletado(s) em ci-diagnostics/api36-groups/$slug/screenshots"
  fi
}

verify_visual_matrix() {
  local name path
  local count=0
  local -a expected=(
    "new-project-phone-dark.png"
    "prepare-ready-dark.png"
    "prepare-search-running-dark.png"
    "settings-dark.png"
    "backup-disconnected.png"
    "backup-empty-connected.png"
    "activity-completed.png"
    "destructive-delete-confirmation.png"
  )

  for name in "${expected[@]}"; do
    path="$(find "$DIAG_ROOT" -type f -name "$name" -print -quit)"
    if [[ -z "$path" || ! -s "$path" ]]; then
      echo "::error title=C8 screenshot matrix::Screenshot obrigatório ausente ou vazio: $name"
      return 1
    fi
    count="$((count + 1))"
  done

  echo "::notice title=C8 screenshot matrix::${count}/${#expected[@]} screenshots obrigatórios coletados com sucesso"
}

collect_runtime_diagnostics() {
  local slug="$1"
  local title="$2"
  local status="$3"
  local dest="$DIAG_ROOT/$slug"
  local file="$dest/runtime-diagnostics.txt"

  mkdir -p "$dest"
  {
    echo "group=$title"
    echo "exitStatus=$status"
    echo "timestampUtc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo
    echo "== adb devices -l =="
    timeout 5s adb devices -l || true
    echo
    echo "== adb get-state =="
    timeout 5s adb get-state || true
    echo
    echo "== sys.boot_completed =="
    timeout 5s adb shell getprop sys.boot_completed || true
    echo
    echo "== instrumentation / app processes =="
    timeout 5s adb shell ps -A 2>/dev/null | grep -E 'studio\.guitarlab\.app|instrumentation|test' || true
    echo
    echo "== focused activity =="
    timeout 5s adb shell dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | head -n 20 || true
    echo
    echo "== top activity =="
    timeout 5s adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' | head -n 20 || true
  } > "$file" 2>&1

  echo "::notice title=API36 diagnostics::Estado do AVD/ADB salvo em ci-diagnostics/api36-groups/$slug/runtime-diagnostics.txt"
}

heartbeat() {
  local index="$1"
  local title="$2"
  local started_epoch="$3"

  while sleep "$HEARTBEAT_SECONDS"; do
    local now elapsed
    now="$(date +%s)"
    elapsed="$((now - started_epoch))"
    echo "::notice title=API36 ${index}/${TOTAL_GROUPS}::${title} ainda executando — $(format_duration "$elapsed") decorridos"
  done
}

run_group() {
  local index="$1"
  local title="$2"
  local slug="$3"
  shift 3

  local dest="$DIAG_ROOT/$slug"
  local log="$dest/gradle.log"
  local started_epoch finished_epoch elapsed duration heartbeat_pid status tests state

  mkdir -p "$dest"
  started_epoch="$(date +%s)"

  echo "::group::API36 ${index}/${TOTAL_GROUPS} — ${title}"
  echo "::notice title=API36 ${index}/${TOTAL_GROUPS}::Iniciando: ${title} · timeout ${GROUP_TIMEOUT_SECONDS}s"

  heartbeat "$index" "$title" "$started_epoch" &
  heartbeat_pid=$!

  set +e
  timeout --signal=TERM --kill-after=30s "${GROUP_TIMEOUT_SECONDS}s" \
    gradle --console=plain --stacktrace :app:connectedDebugAndroidTest "$@" \
    2>&1 | tee "$log"
  status=${PIPESTATUS[0]}
  set -e

  kill "$heartbeat_pid" 2>/dev/null || true
  wait "$heartbeat_pid" 2>/dev/null || true

  finished_epoch="$(date +%s)"
  elapsed="$((finished_epoch - started_epoch))"
  duration="$(format_duration "$elapsed")"

  copy_group_evidence "$slug"
  collect_visual_evidence "$slug"
  tests="$(count_testcases "$dest/results")"

  if [[ "$status" -eq 0 ]]; then
    echo "| ${index}/${TOTAL_GROUPS} — ${title} | ✅ PASS | ${tests} | ${duration} |" >> "$SUMMARY_FILE"
    echo "::notice title=API36 ${index}/${TOTAL_GROUPS}::PASS: ${title} · ${tests} testes observados · ${duration}"
    echo "::endgroup::"
    return 0
  fi

  collect_runtime_diagnostics "$slug" "$title" "$status"

  if [[ "$status" -eq 124 || "$status" -eq 137 ]]; then
    state="⏱️ TIMEOUT"
    echo "::error title=API36 ${index}/${TOTAL_GROUPS}::TIMEOUT: ${title} excedeu ${GROUP_TIMEOUT_SECONDS}s. Diagnóstico ADB/AVD capturado."
  else
    state="❌ FAIL"
    echo "::error title=API36 ${index}/${TOTAL_GROUPS}::FAIL: ${title} (exit ${status}). Diagnóstico ADB/AVD capturado."
  fi

  echo "| ${index}/${TOTAL_GROUPS} — ${title} | ${state} | ${tests} | ${duration} |" >> "$SUMMARY_FILE"
  echo "::endgroup::"
  return "$status"
}

verify_test_coverage

normalize_emulator_ui() {
  echo "::notice title=API36::Normalizando estado gráfico do AVD antes da regressão"
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
  adb shell settings put system accelerometer_rotation 1 >/dev/null 2>&1 || true
  adb shell settings delete system user_rotation >/dev/null 2>&1 || true
  adb shell settings put secure immersive_mode_confirmations confirmed >/dev/null 2>&1 || true
  adb shell am force-stop studio.guitarlab.app >/dev/null 2>&1 || true

  {
    echo "== normalized display state =="
    adb shell wm size || true
    adb shell wm density || true
    echo "immersive_mode_confirmations=$(adb shell settings get secure immersive_mode_confirmations 2>/dev/null | tr -d '\r' || true)"
    echo "accelerometer_rotation=$(adb shell settings get system accelerometer_rotation 2>/dev/null | tr -d '\r' || true)"
  } | tee "$DIAG_ROOT/normalized-avd-state.txt"
}

restore_emulator_ui() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
  adb shell settings put system accelerometer_rotation 1 >/dev/null 2>&1 || true
  adb shell settings delete system user_rotation >/dev/null 2>&1 || true
  adb shell settings put secure immersive_mode_confirmations confirmed >/dev/null 2>&1 || true
}

echo "::notice title=API36::Validando conexão com o emulador"
adb wait-for-device
normalize_emulator_ui
trap restore_emulator_ui EXIT
echo "::notice title=API36::Emulador conectado e normalizado; iniciando ${TOTAL_GROUPS} grupos"

run_group \
  1 \
  "Projeto e navegação" \
  "01-project-navigation" \
  "-Pandroid.testInstrumentationRunnerArguments.class=$(class_filter "${GROUP1_CLASSES[@]}")"

run_group \
  2 \
  "Studio e prática" \
  "02-studio-practice" \
  "-Pandroid.testInstrumentationRunnerArguments.class=$(class_filter "${GROUP2_CLASSES[@]}")"

run_group \
  3 \
  "Importação, controles e ajustes" \
  "03-import-controls-settings" \
  "-Pandroid.testInstrumentationRunnerArguments.class=$(class_filter "${GROUP3_CLASSES[@]}")"

run_group \
  4 \
  "Exportação, backup e master" \
  "04-export-backup-master" \
  "-Pandroid.testInstrumentationRunnerArguments.class=$(class_filter "${GROUP4_CLASSES[@]}")"

echo "::notice title=API36 5/${TOTAL_GROUPS}::Configurando viewport tablet 1920×1200"
adb shell wm size 1920x1200
adb shell wm density 240
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb shell am force-stop studio.guitarlab.app || true

run_group \
  5 \
  "Geometria tablet 1920×1200" \
  "05-tablet-geometry" \
  "-Pandroid.testInstrumentationRunnerArguments.class=$(class_filter "${GROUP5_CLASSES[@]}")" \
  "-Pandroid.testInstrumentationRunnerArguments.targetGeometry=true"

verify_visual_matrix

{
  echo
  echo "**Resultado API36:** 5/5 grupos PASS."
} >> "$SUMMARY_FILE"

echo "::notice title=API36::5/5 grupos funcionais concluídos com sucesso"
