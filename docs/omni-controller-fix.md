# Omni Speed Controller recursion fix

Applies when Create **6.0.10** (including build suffixes such as `6.0.10-280`) and Dreams n' Desires **2.3a-BETA** are installed. The mixin plugin skips the integration otherwise; Dreams n' Desires is not a new mandatory dependency.

The affected controller computes an output ratio from its requested RPM and effective input speed. Create can interpret output-side feedback as a stronger input, replace the source, recompute the ratio, and recurse into `propagateNewSource` without reaching a stable network. Effective speed also becomes zero under overstress.

The integration mirrors Create's **Rotation Speed Controller** pattern:

1. Drops established Omni controllers from an output neighbour's connection list so Create never walks reverse into them (zero conveyed speed alone is not enough — Create's fallback branch can still replace the source).
2. Reports zero conveyed speed for output → Omni feedback.
3. For Omni → ordinary ±1 shaft/gearbox receivers, emits the scrolled target RPM directly (same idea as `SpeedControllerBlockEntity.getDesiredOutputSpeed`), without requiring a non-zero theoretical input on that outbound path.
4. Keeps face ratios on the modifier path based on theoretical input speed so overstress (effective speed 0) does not produce invalid ratios.

Non-Omni connections and nonadjacent connections are left to Create. Existing mechanical conflict handling is retained.

## Validation

- `gradlew.bat build --offline --no-daemon`: build and JUnit suite pass.
- Isolated NeoForge dedicated-server checks: original failing chunk no longer StackOverflows; motor/controller/shaft fixtures retain working output at the scrolled target.

## Gearshift direction diagnosis (2026-11-07)

The target is signed RPM, matching Create's ordinary speed controller: an Omni set to +16 RPM emits +16 RPM even if its input reverses. Place a gearshift after the last speed controller to reverse the final load. A second Omni after that gearshift establishes its own signed output again.

An isolated native GameTest (`ccq_omni_gearshift`) exercises 30 circuits: all six shaft directions, upstream gearshifts, immediately adjacent downstream gearshifts, downstream gearshifts with an intermediate shaft, two-controller chains, and immediately downstream clutches. It checks repeated on/off changes and positive/negative targets. All circuits passed using the 0.4.112 runtime mixins. Downstream gearshifts change +16 to -16 and -32 to +32; upstream gearshifts and gearshifts between two controllers do not override the final signed target. Clutches after Omni stop the load.

No production code change or version bump was made for this diagnosis. These results do not identify the topology of a user's uninspected circuit.

### Bunker docking control and saved-world follow-up

The user identified the retractable breech bridge, not an upstream gearshift. Production 0.4.112 was confirmed in SER's last startup log. Added Azimuth, Bits 'n' Bobs/Struts, Create Lazy Tick (with SER's configuration), Create Radiologistics and Simulated Coasters to the isolated mechanical runtime: all seven required tests passed.

Read the closed SER save without editing it and copied only the relevant test-ground region/entity files into a separate test server. Exercised the actual saved green/iron-nugget docking control: transmitter and receiver reached signal 15, the powered gearshift retained input Speed=-8 while the piston received +8 RPM and reached offset 3. On release the piston returned to -8 RPM and offset 0. This saved circuit reload test passed. Thus the reported failure was not reproduced; a gearshift's own input Speed remaining unchanged does not imply its output failed to reverse. No production save, mod JAR or CF files were changed for this diagnosis.
