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
