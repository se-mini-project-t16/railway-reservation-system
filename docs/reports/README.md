# Part 3 test reports

Each report is a Word document with a PDF export of the same content.

| Report | Word | PDF |
|---|---|---|
| Unit Test Report | [docx](unit-test-report.docx) | [pdf](unit-test-report.pdf) |
| System Validation Report | [docx](system-validation-report.docx) | [pdf](system-validation-report.pdf) |
| Code Coverage Report | [docx](code-coverage-report.docx) | [pdf](code-coverage-report.pdf) |
| Branch Coverage Report | [docx](branch-coverage-report.docx) | [pdf](branch-coverage-report.pdf) |

Latest run, 9 November 2026: 199 tests, all passing. Line coverage 84.7%, branch coverage 69.0%. The system
validation report also covers the usability test on the live site (8 November), the Postman collection run, a
load test, security checks, an idle timeout check in a real browser and an accessibility sweep, all against the
final build.

`coverage-data/` holds the raw JaCoCo output (CSV and XML) the coverage reports were made from.
`screenshots/` holds the images used in the system validation report.

The numbers come from a normal build:

```bash
./mvnw clean test                       # JaCoCo writes target/site/jacoco (HTML, XML, CSV)
./mvnw surefire-report:report-only      # Maven's HTML test report in target/reports/surefire.html
```
