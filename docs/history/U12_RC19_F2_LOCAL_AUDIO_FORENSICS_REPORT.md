# U12 RC19 — F2 local audio forensics report

Date: 2026-09-24  
Inputs: user-exported active backing/guitar references from two physical projects

## WATG — Deadbolt

- backing SHA-256: `d9f206f25f9eb28d02c7fb4a4a3e1d667077429e91520eb64763cd0251fe2a8e`;
- guitar SHA-256: `8bbb4b5eb6b4ef8758669c1734b8d095893aadc9792d12a6330d9318eae725fa`;
- finite samples: all; non-finite samples: zero;
- backing mean/DC: `-0.04729255`, `-0.04737955`;
- guitar mean/DC: `-0.00903160`, `-0.00906088`;
- backing RMS: `0.21881498`, `0.21973251`;
- guitar RMS: `0.12903734`, `0.12725534`;
- recombined peak: `0.8912509363` / `-1.00000002 dBFS`.

## MMF — Misery

- backing SHA-256: `a80755bea5665d6daa1e8a7257918d4bc36972fb8d970075e2ae01b35c7fff2e`;
- guitar SHA-256: `c52dea43d70b58daf8e1df941ac286a4f6732d8c127041c36bc7940a3b7cad38`;
- finite samples: all; non-finite samples: zero;
- backing mean/DC: `-0.06221448`, `-0.06223602`;
- guitar mean/DC: `-0.01242727`, `-0.01251480`;
- backing RMS: `0.38768636`, `0.38805340`;
- guitar RMS: `0.07512788`, `0.07514008`;
- recombined peak: `0.8912509233` / `-1.00000014 dBFS`.

## Conclusion

Both independent physical projects contain substantial same-polarity DC in both
prepared deliverables. Misery reproduces and exceeds Deadbolt's DC magnitude.
Both pairs remain finite, aligned and constrained by the v2 shared-gain ceiling.

This rejects a Deadbolt-only/source-specific explanation and makes a local
playback-only defect unlikely: the contamination is present in the exported WAV
sample values. Combined with F1's fresh Cloud Run executions and deterministic
same-source remote hashes, the remaining boundary is worker-side canonical input,
Demucs output/normalization, or prepared-reference rendering. Source and per-stem
metrics are required to distinguish these stages. No DC blocker is authorized
until that boundary is measured.

Machine-readable full metrics:

- `artifacts/u12-forensics/watg-deadbolt-local-audio.json`;
- `artifacts/u12-forensics/mmf-misery-local-audio.json`.
