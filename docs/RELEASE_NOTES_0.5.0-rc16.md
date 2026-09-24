# GuitarLab Studio 0.5.0-rc16 — Release Notes

Version: `0.5.0-rc16` / versionCode `36`

RC16 corrects the two findings from RC15 physical homologation: unsafe prepared-reference audio
could reach playback without signal-level validation, and clearing the prepared clips could leave
their bindings behind, preventing a later separation from repopulating the Studio.

Remote Base/Guitar delivery is now checked at both boundaries. Cloud Run validates the actual
peaks of Base, Guitar and their recombination after gain rendering, and rejects catastrophic
degradation by measuring sampled reconstruction SNR between the canonical source and the sum of
all six normalized stems. The numerical result remains in structured Cloud logs for diagnosis
without retaining user audio. Android verifies the remote
hash/container, decodes float samples strictly, rejects non-finite or over-ceiling audio and writes
the accepted pair into GuitarLab's canonical float32 WAV before atomic publication. The Studio
therefore never plays a remote container directly.

Prepared-reference synchronization now treats a binding without its clip as incomplete. A new
separation automatically fills empty reference lanes even when stale bindings exist, while the
existing explicit Studio/media action can reinsert or update Base and Guitar without another
download or Demucs execution. Recordings, takes, mixer state and other creative content remain
untouched.

RC16 requires exact-source Android CI, U7 backend verification, controlled production worker
deployment/U4 real-cloud acceptance, and physical listening plus delete/reinsert acceptance before
promotion.
