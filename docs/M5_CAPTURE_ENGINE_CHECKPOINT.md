# M5 Capture Engine Checkpoint

Status: implementation checkpoint pending branch-head CI. Physical recording homologation remains pending.

## Scope
This checkpoint wires the Android capture foundation required by M5 without yet claiming the complete Studio recording workflow.

Implemented:
- `MonitoringMode`: `OFF`, `AUTO`, `ON`;
- conservative, unit-tested software-monitoring policy;
- Android `AudioRecord` capture engine with FLOAT32 preference and PCM16 fallback converted to normalized float;
- mono-first input negotiation, stereo fallback;
- exact project sample-rate request when supplied;
- preferred global input routing using the device resolved from the existing stable-signature Options preference;
- explicit selected-input absence is a hard failure rather than silent substitution;
- route-change detection while recording;
- streaming 32-bit float WAV writing into the uncommitted recording transaction;
- live input Peak/RMS reporting;
- valid partial-take result on route/input failure after frames have already been captured;
- zero-frame/invalid capture reports an error instead of producing a take;
- optional software monitoring through a low-latency stereo `AudioTrack`;
- Options persistence and pt-BR selector for monitoring mode.

## AUTO monitoring policy
`AUTO` is deliberately conservative:
- USB input: software monitoring off by default to avoid doubled direct monitoring;
- Bluetooth output: off because latency is unsuitable for live guitar monitoring;
- built-in input to wired output: software monitoring may be enabled;
- `ON` explicitly requests software monitoring;
- `OFF` never software-monitors.

Monitoring failure never aborts an otherwise valid recording; it is reported as a warning and capture continues.

## Safety boundaries
- captured bytes remain in a temporary project recording file until the app layer validates/finalizes the take;
- the engine never overwrites an existing managed source;
- source promotion/project metadata insertion remains the responsibility of the M5 Studio coordinator;
- measured round-trip latency compensation remains M6;
- no physical recording support is advertised until the complete M5 gate passes.

## Next checkpoint
M5 Studio coordinator:
1. request/verify Android recording permission;
2. require armed track(s);
3. run the mandatory 5-second Record countdown;
4. start capture and optional backing playback;
5. surface live input meter/clipping;
6. Stop/finalize/validate the take;
7. atomically promote it into `media/source/`;
8. create clip metadata + waveform for armed tracks;
9. recover/discard interrupted sessions safely.
