# Omni Speed Controller recursion fix

Applies only when Create **6.0.10** and Dreams n' Desires **2.3a-BETA** are installed. The mixin plugin skips the integration otherwise; Dreams n' Desires is not a new mandatory dependency.

The affected controller computes an output ratio from its requested RPM and effective input speed. Create can interpret output-side feedback as a stronger input, replace the source, recompute the ratio, and recurse into `propagateNewSource` without reaching a stable network. Effective speed also becomes zero under overstress.

The integration prevents adjacent output shaft connections from replacing an established Omni input, computes ratios from theoretical input speed, handles zero/uninitialised inputs, and returns exact requested RPM for ordinary receiving shaft/gearbox modifiers. Non-Omni connections and nonadjacent connections are left to Create. Existing mechanical conflict handling is retained.

## Validation

- `gradlew.bat build --offline --no-daemon`: build and JUnit suite pass. Regression calculations cover signed and fractional inputs, zero output, stopped/nonfinite input and the saved `-64 -> -16 RPM` controller state.
- Isolated NeoForge 21.1.255 / Java 21 dedicated server using the pack's 242-mod server set.
- Loading a copy of the original failing chunk with CCQ Core 0.4.62 reproduces `StackOverflowError` immediately.
- Loading the same original chunk with 0.4.63 no longer causes recursive failure. Conflicting powered networks may still break the controller through Create's normal protection.
- A separate motor/controller/shaft fixture checks that a valid connection retains a working controller and output shaft.

The private world backup and its player data are not included in the repository or release. Validation does not establish compatibility with future upstream implementations or every third-party custom transmission.
