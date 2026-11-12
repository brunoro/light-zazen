---
title: Explore synthesized zazen bell sounds
status: open
type: feature
---

## Objective

Explore a convincing synthesized zazen bell, with reproducible sound generation and expressive strikes. Deferred work: audition candidates before integrating any replacement into the app.

## Current behavior

The app plays a roughly 2.5-second CC0 recording. Opening strikes use 75%, 100%, and 50% gain, with independently randomized 4–6-second strike intervals. The closing strike uses full gain. Vibration follows the same intensity progression. Audio variants currently differ only in gain.

## Research

- [Gordon Reid: Synthesizing Bells, Sound On Sound](https://www.soundonsound.com/techniques/synthesizing-bells). Full article and patch screenshots supplied in the project conversation. Uses a brief FM impact, six independently tuned sine partials for the body, decay envelopes, amplitude modulation to approximate beating, and a low sustained hum. Primarily describes Western tuned bells; its frequency ratios and octave hum should not be assumed appropriate for a zazen bowl. The patch is tuned for a limited pitch range.
- [Oguchi and Samejima: Physical modeling of Japanese temple bells using thin cylindrical shells involving dynamics of clappers Shumoku (2023)](https://www.jstage.jst.go.jp/article/ast/44/3/44_E2257/_pdf/-char/en). Models a hanging temple bell and wooden striker with shell mechanics and numerical methods. Demonstrates the influence of striker properties and geometry on the spectrum. Useful physical guidance; implementing its full simulation would be substantially more work than a modal synthesis prototype and would not directly model a bowl struck at its rim.
- [Spectral Directivity of Singing Bowls, DAGA 2014](https://pub.dega-akustik.de/DAGA_2014/data/articles/000254.pdf). Reference for modal pairs and characteristic beating.
- [Real-Time Spatial Processing and Transformations of a Singing Bowl, DAFx](https://dafx.de/paper-archive/details/JkPJBVtRRg6FFlvZWRn_tw). Further reference for physical and modal modeling of bowls.

## Requirements

- Produce an offline, reproducible synthesis script and auditionable WAV files.
- Combine inharmonic resonances with independent amplitudes and decay times, a short mallet attack, and subtle beating from closely spaced resonance pairs.
- Make medium, full, and soft strikes differ in attack and brightness as well as volume.
- Preserve a natural decay without clipping, clicks, or an abrupt cutoff.
- Compare on headphones and the Light Phone III speaker before selecting a sound.
- Record generation parameters and asset provenance.
- Keep app integration contingent on approval of an auditioned candidate.

## Plan

1. Choose a target character: small bowl with a clear tone or larger bell with deep resonance. Use a reference recording to guide tuning where available.
2. Build a parameterized offline additive/modal synthesis prototype. Start with independent decaying sine resonances; add detuned pairs and a short impact excitation.
3. Render warm, balanced, and bright candidates, including medium/full/soft strike variations and a sample opening sequence.
4. Audition, compare with the existing recording, and iterate on attack, beating, brightness, and decay.
5. After sound approval, plan app integration. Current playback queues each bell followed by silence, so long tails cannot overlap subsequent strikes. Evaluate rendering the entire opening sequence with overlapping tails while preserving timing variation and synchronized vibration.
6. Document the selected parameters and build process; update asset credits if integrated.

## Validation

- Check generated WAV format, duration, peak level, finite samples, and clean beginnings/endings.
- Listen for natural attack, smooth resonance, restrained beating, and distinct strike dynamics.
- Listen to the opening sequence for overlapping tails and timing.
- Verify audibility and tonal balance on the connected device.
- If integration proceeds, validate session timing, cancellation, locked-device playback, and vibration synchronization; run relevant tests and rebuild.

## Unresolved questions

- Small bowl or deep bell?
- Match the current recording or design a new timbre?
- Desired decay length?

## Effort assessment

A basic synthesis prototype is moderate effort. Achieving a convincing tone will require listening and tuning. Full geometric physical simulation is research-level work and is outside the initial experiment.
