# Studio Options, Export and Mixer Contract

Updated: 2026-10-02

## Options
Options owns low-frequency setup and diagnostics: input route, main output, secondary/CUE output, monitoring, Studio preferences, import capability information and diagnostic tools. These controls stay out of the timeline.

MAIN may remain Android-selected for ordinary single-output work. CUE is explicit-only: it can be selected only when MAIN is explicit, must resolve to a different live endpoint and has no automatic fallback. Synchronized CUE intentionally excludes Bluetooth because wireless presentation latency is too large/variable for the MAIN↔CUE alignment contract; both MAIN and CUE must be wired/USB/other low-latency destinations. Bluetooth remains valid for ordinary single-output MAIN. Selection alone does not activate CUE: the playback engine first confirms distinct routes plus stable presentation clocks and admits CUE only when the initial MAIN↔CUE stream-origin offset is at most 12 ms. If that proof fails, or the live drift later exceeds the bounded envelope, the CUE bus is silenced while MAIN continues whenever safe.

Project persistence and final-audio export are **not** Options actions.

## Studio global actions
Studio exposes Mixer, Help, Options, Export and Home using the shared project-shell navigation language. Comparison, Timeline and Níveis use three fixed-size transport-navbar triggers. Comparison and Timeline keep their anchored vertical panels (240 dp and 320 dp); Níveis opens the all-tracks level-analysis modal directly. Equal reserved left/right groups keep transport centered on the viewport. A single fixed mode slot switches Mixer minimum/complete; it is disabled, not removed, when hidden. Comparison stays open for repeated A/B choices. Timeline keeps creation and destructive cleanup separate and explains the missing-loop prerequisite. No redundant Adjustments, Master-level Níveis button or Pin trigger remains. Narrow windows scroll the same single navbar row.

`Ajuda` opens the same shared `StudioUserGuideDialog` implementation used by the Home entry point; there is no second guide implementation/copy.

## Project persistence and Export
Portable `.guitarlab` save remains semantically distinct from external Study Export and Studio Master delivery. The canonical Export workspace owns output/format selection; Studio/Home entry points navigate there and do not maintain a second modal chooser. Sample rate follows established project/render policy without unnecessary codec knobs.

## Mixer
Mixer always reserves its own bottom layout space: visibility and minimum/complete mode are durable, independent preferences. Default and upgrades start complete. Legacy `mixer_visible`/`mixer_pinned` visibility is retained; RC23 floating/expanded flags are not mapped to minimum because both old heights exposed complete controls. A mode write retires those obsolete flags.

At normal font scale channels are 200 dp (original 232 dp), with all four Mute/Solo/CUE/REC targets contiguous in one centered row: 48×48 dp touch regions and 46×46 dp visible faces. Channel padding is 4 dp per side. The track accent dot + title are centered as one header identity group. Header, state actions, metering and mix controls are separated by low-contrast 6 dp section surfaces without adding vertical budget. Master is a separate fixed 144 dp column; channels scroll horizontally without covering it. Master centers its title and uses the same section language for header, metering and volume, but never displays an empty/fake action bank. Font scaling grows channel width and header/meter height rather than overlapping touch regions.

Complete height is 252 dp, down from RC23's 352 dp (28.4%). All four state controls, PK/RMS/numeric readings/peak hold/CLIP reset, gain and bipolar pan are exposed. The complete card keeps four geometry-stable visual zones: centered header, state-action bank, meter bank and a 96 dp mix bank containing the 48 dp Volume/Pan rows with a soft internal divider. Gain and pan retain Material Slider gesture, keyboard and accessibility behavior. PK/RMS consume the full inner strip width; CLIP reset remains in the header so clipping never steals permanent meter width.

Minimum height is 172 dp. It retains the centered header, all four direct state controls, gain/readout, a compact peak/hold meter with accessible PK/RMS readings, pan value and clipping indication. The same soft section grammar is preserved without reserving space for hidden controls. Tapping the track header opens the same complete strip implementation in a details dialog, exposing pan and CLIP reset with the same domain callbacks/transport locks. Master retains gain and compact metering but no Níveis button or fake action section; its wide volume slider places `VOL` and the dB value together on one line below the slider. Clipping resets from the header. Mode changes never issue audio commands. Gains/pans preview live and commit on gesture completion; no separate mix model exists for minimum/details.

Each track persists an output-route value with backward-compatible default `MAIN`. The engine contract supports `MAIN`, `CUE` and `MAIN_AND_CUE`; the first user-facing headphone control intentionally toggles exclusive MAIN ↔ CUE. New MAIN→CUE activation requires an available explicit low-latency pair; rejection preserves MAIN and history. Returning CUE→MAIN remains possible even when a device disappears. Route changes are accepted only while the transport is stopped, so Play/REC cannot leave track metadata and live-monitor routing out of sync. The route is restored with the project, applied to normal playback and backing playback during recording, and does not destructively change clips/takes.

CUE is a secondary monitoring/playback bus. The secondary sink is opened only when currently playable content belongs to a CUE-routed track; an empty CUE track does not create an unnecessary output stream. Its writer is non-blocking relative to MAIN: if the secondary sink cannot accept a complete render chunk, CUE is silenced rather than allowed to stall the primary output. CUE is not an export exclusion flag. Studio Master export continues to render the project timeline/mix semantics independently of the live physical CUE assignment.

## Timeline/track UI
- track names: canonical 1–24 chars;
- `Limpar pista`: track-content action; removes clips but preserves track identity/function/color/order/mix;
- `Excluir clipe`: clip-segment action; removes only the selected clip after confirmation and preserves valid sibling take/media references;
- drag-to-trash is a direct-manipulation shortcut into the same confirmed clip-deletion domain command;
- `Excluir pista`: structural action inside `Configurar pista`; safe/disabled as required for populated tracks;
- source metadata lives in `Configurar pista`, responsive to available width;
- a populated track uses a clear edit affordance for its content/configuration path;
- track ordering remains drag-only through the workspace coordinator;
- clip migration/deletion resolves through transaction-safe drag intent (`Move`, `Delete`, `NoOp`).

## Visual hierarchy invariants

- header identity is centered as a group regardless of selected state; transient trailing status such as comparison/clip may not permanently displace the normal title geometry; the visible CLIP warning face must not overlap the centered title even though its transparent/tappable parent retains a 48 dp target;
- each visible section spans the useful inner card width and remains vertically ordered/non-overlapping;
- soft segmentation is presentation-only: Mute/Solo/CUE/REC, gain, pan, metering, CLIP and Master callbacks remain the same domain actions;
- normal-font dimensions remain 200 dp track / 144 dp Master / 252 dp complete / 172 dp minimum;
- selected and unselected tracks remain distinguishable by border/tonal treatment as well as color;
- Master follows the same visual grammar without placeholder controls.

## Qualification
Repeat focused Mixer/Options/Export regression when a candidate changes those surfaces or their domain commands. A Mixer visual-hierarchy candidate must additionally assert centered headers, section width/order, retained 48 dp targets, large-font containment and five-channel target-tablet fit, then inspect complete/minimum screenshot artifacts. They do not create a global release or maintenance gate for unrelated worker/search changes.

## RC25 route startup correction

The media-output selector excludes Bluetooth SCO (call profile), retaining A2DP/LE media and preserving legitimate mono USB outputs. Recording inputs are unchanged. An old SCO output preference migrates only to one unambiguous matching product/address media route; ambiguities remain unavailable instead of selecting another headset. Candidate ids never include SCO.

Dual-output admission prefills silence, then feeds both AudioTracks with non-blocking writes through a bounded route-settlement phase of up to 5 s followed by a separate clock-qualification phase of up to 2 s. Repeated timestamps do not count, three independent stable observations per sink are required, route convergence and >12 ms initial offset reject CUE, cancellation exits promptly, and cleanup restores volume/flushes probe buffers. No musical samples are used in admission. Runtime queues are primed again after probe cleanup; CUE writes remain non-blocking and route listeners complement per-chunk route/drift guards. Android preferred-device acceptance is not proof of simultaneous physical routing; hardware pairs require owner acceptance.

A seleção secundária é validada imediatamente em Opções: mostra “Verificando MAIN + CUE”, abre duas saídas com buffers zerados e volume zero e somente persiste CUE depois de comprovar rotas distintas e clocks estáveis. O controlador diferencia fone ausente, convergência para MAIN, rota errada/espelhada, clock indisponível e offset excedido; falhas mantêm CUE desativado e permitem teste explícito novamente. Nova seleção, alteração de MAIN e qualquer callback de adição/remoção de dispositivo cancelam resultados pendentes e invalidam o cache da sessão. Diagnósticos de latência não rodam em paralelo com esse teste. O teste usa a taxa do projeto disponível (48 kHz fora do projeto); mede evidência de apresentação relativa, não latência acústica/round-trip. Play/REC ainda verifica os streams reais e mudanças posteriores; uma aprovação anterior não garante suporte permanente do hardware.

Os sliders compactos reservam 48dp de altura interativa, com thumb visual de 6×20dp centralizado. A margem horizontal interna de 10dp acomoda a expansão semântica do Material Slider, permitindo alcançar todo o controle nos extremos da rolagem. Isso se aplica a volume, pan e Master sem aumentar a altura dos docks ou a largura dos canais.

## RC27b clipping affordance

Clipping reset remains a header action so PK/RMS keep the full-width RC26/RC27 meter contract. Its semantic/click target remains 48×48 dp with the same `Limpar clipping…` description and callback. Only the visible face is compacted to a 28×24 dp warning badge, preventing centered track/Master identity text from colliding with the transient clipping affordance at normal and large font scales.

## RC27c clipping badge placement

The clear-clipping action retains its 48×48 dp semantic/touch target and 28×24 dp visible warning face. The face is aligned to the outer/right edge of that target rather than centered inside it, preventing the transient warning from intruding into the centered identity region. This is purely visual placement; reset behavior and accessible target size are unchanged.


## RC28 CUE physical-route identity

The selector and verifier now use the same physical-route abstraction. Android numeric device ids remain transient logical endpoint ids, not persisted or treated as physical identity. On API 36 the verifier inspects the complete `AudioTrack.routedDevices` set: several logical endpoints are accepted only when every one canonicalizes to the explicitly selected physical destination. This covers legitimate MK-300 USB endpoint aliases without weakening isolation.

CUE still fails closed when route evidence is absent, MAIN and CUE canonicalize to the same physical destination, either stream is mirrored to an additional physical destination, clocks do not stabilize, initial presentation offset exceeds 12 ms, or runtime drift/backpressure guards fail. Selection-time verification and Play use the same physical-route rule so a route cannot pass Settings and then be rejected merely because Android reports a sibling logical endpoint during playback.
