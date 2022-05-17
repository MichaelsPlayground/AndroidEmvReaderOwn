This app is been able to read data from EMV cards (NFC enabled Credit Cards).

It uses the library EMV NFC Paycard Enrollment written by GitHub user devnied.

To use the library, simply use Gradle with this line:

implementation 'com.github.devnied.emvnfccard:library:3.0.1'

As the documentation is not the best one I searched a lot and could find only ONE (1!) 
example on Stackoverflow that shows how to embedd the lib:

https://stackoverflow.com/questions/58825020/kotlin-emv-android-nfc-tag-setconnectedtechnology-error

There is just one activity that shows all data revelead from the lib in a TextView.

Kindly note 2 points:

a) as per MasterCard/VisaCard's directive the cardholder name is no longer available on the chip and you 
will recieve a "null" String

b) I can't get out any BIC/IBAN on MaestroCards/German Geldkarte

A last note: all fields could be null so don't forget to run sanity checks before accessing data returned by the lib.

Greetings

Michael
