# User Guide Policy

Updated: 2026-10-01

## Purpose

User-facing help is part of the supported product surface. It must explain the owner's actual workflow without exposing obsolete architecture, duplicate instructions or developer-only implementation detail.

## Single-guide rule

Home and Studio open the same shared `StudioUserGuideDialog` content. Do not create a second competing guide whose copy can drift.

## Required coverage

The guide should describe, at the level needed for normal use:

- project creation/opening;
- `Preparar` source acquisition and separation;
- transition to `Studio`;
- prepared Base/Guitar references and local reference restoration;
- recording target/REC Arm, recording and takes;
- timeline/trim/split/loop/practice behavior;
- Mixer, audition modes and per-track MAIN/CUE headphone routing;
- `Exportar`;
- Activity/cancellation where useful;
- backup/restore and account/cloud dependency at a high level;
- where Diagnostics lives when troubleshooting is needed.

The guide must not claim unsupported tuner/pitch functionality or retired legacy migration behavior.

## Copy and terminology

Follow `UI_COPY_STYLE.md`. Use product language first; Firebase, Cloud Run, WorkManager, SHA hashes and internal state-machine names belong in Diagnostics or engineering documentation unless the user must know them to act safely.

Canonical project-workspace names are `Preparar`, `Studio` and `Exportar`.

## Change discipline

A user-visible workflow change updates the shared guide in the same development block when the existing instructions would otherwise become wrong or materially incomplete. Pure implementation changes that do not alter user action do not require guide churn.

## Accessibility and offline behavior

Guide content must remain readable at supported text scaling, scroll safely on tablet layouts and not require network access merely to read the instructions.

## Qualification

When help content/navigation changes, verify both Home and Studio open the same guide and that no stale/duplicate guide path remains.
