from pathlib import Path

p = Path('tools/m7_integrate.py')
t = p.read_text()

diag_old = 'raise SystemExit(f"{path}: anchor not found enough times ({found} < {count})")'
diag_new = 'raise SystemExit(f"{path}: anchor not found enough times ({found} < {count}); anchor={old[:180]!r}")'
if diag_old in t:
    t = t.replace(diag_old, diag_new, 1)

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
p.write_text(t)
