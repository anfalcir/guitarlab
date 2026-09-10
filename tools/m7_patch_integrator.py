from pathlib import Path

p = Path('tools/m7_integrate.py')
t = p.read_text()

diag_old = 'raise SystemExit(f"{path}: anchor not found enough times ({found} < {count})")'
diag_new = 'raise SystemExit(f"{path}: anchor not found enough times ({found} < {count}); anchor={old[:180]!r}")'
if diag_old in t:
    t = t.replace(diag_old, diag_new, 1)

# StudioViewModel source/editing-rate metadata block: actual source field order on M6 baseline.
start = "replace(p, '''                            sourceSampleRateHz = metadata.sampleRateHz,"
end_marker = "# Import status should report"
a = t.find(start)
b = t.find(end_marker, a)
if a < 0 or b < 0:
    raise SystemExit('StudioViewModel source metadata replacement block not found')
replacement = """replace(p, '''                            sourceTotalFrames = metadata.totalFrames,
                            sourceFormat = originalFormat.displayName,
                            sourceSampleRateHz = metadata.sampleRateHz,
                            sourceChannelCount = metadata.channelCount,
                            sourceBitsPerSample = sourceBits,
                            sourceEncoding = sourceEncoding,
''', '''                            sourceTotalFrames = sourceMetadata.totalFrames,
                            sourceFormat = originalFormat.displayName,
                            sourceSampleRateHz = sourceMetadata.sampleRateHz,
                            sourceChannelCount = sourceMetadata.channelCount,
                            sourceBitsPerSample = sourceBits,
                            sourceEncoding = sourceEncoding,
                            editingSampleRateHz = metadata.sampleRateHz,
                            editingTotalFrames = metadata.totalFrames,
''')
"""
t = t[:a] + replacement + t[b:]

# StudioTrackLane's actual signature places trackIndex immediately after onSplitStereo.
bad = "replace(p, '''    onSplitStereo: (String) -> Unit,\n    onTrackDragStart:''', '''    onSplitStereo: (String) -> Unit,\n    onOpenFades: (String) -> Unit,\n    onCrossfade: (String) -> Unit,\n    onTrackDragStart:''')"
good = "replace(p, '''    onSplitStereo: (String) -> Unit,\n    trackIndex: Int,''', '''    onSplitStereo: (String) -> Unit,\n    onOpenFades: (String) -> Unit,\n    onCrossfade: (String) -> Unit,\n    trackIndex: Int,''')"
if bad not in t:
    raise SystemExit('StudioTrackLane callback transform anchor not found in integrator')
t = t.replace(bad, good, 1)

p.write_text(t)
