package de.androidcrypto.androidemvreaderown;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;

import com.github.devnied.emvnfccard.enums.EmvCardScheme;
import com.github.devnied.emvnfccard.model.Application;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.model.EmvTrack1;
import com.github.devnied.emvnfccard.model.EmvTrack2;
import com.github.devnied.emvnfccard.model.EmvTransactionRecord;
import com.github.devnied.emvnfccard.model.Service;
import com.github.devnied.emvnfccard.model.enums.CardStateEnum;
import com.github.devnied.emvnfccard.parser.EmvTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    // compile 'com.github.devnied.emvnfccard:library:3.0.1' does not work
    // implementation 'com.github.devnied.emvnfccard:library:3.0.1' is ok
    // starting code inspired by https://stackoverflow.com/questions/58825020/kotlin-emv-android-nfc-tag-setconnectedtechnology-error
    // answer:

    TextView nfcaContent;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcaContent = findViewById(R.id.tvNfcaContent);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        // clear the datafields
        //clearEncryptionData();

        System.out.println("NFC tag discovered");

        // NfcA nfca = null; // changed to IsoDep
        IsoDep isoDep = null;

        // Whole process is put into a big try-catch trying to catch the transceive's IOException
        try {

            isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150,10));
            }

            isoDep.connect();
            byte[] response;
            String idContentString = "Content of ISODEP tag";

            // first get historical bytes
            response = isoDep.getHistoricalBytes();
            idContentString = idContentString + "\n" + "historical data length: " + response.length;
            idContentString = idContentString + "\n" + "Data: " + bytesToHex(response);

            PcscProvider provider = new PcscProvider();
            provider.setmTagCom(isoDep);

            EmvTemplate.Config config = EmvTemplate.Config()
                    .setContactLess(true)
                    .setReadAllAids(true)
                    .setReadTransactions(true)
                    .setRemoveDefaultParsers(false)
                    .setReadAt(true);

            EmvTemplate parser = EmvTemplate.Builder()
                    .setProvider(provider)
                    .setConfig(config)
                    .build();

            EmvCard card = parser.readEmvCard();
            String cardNumber = card.getCardNumber();
            Date expireDate = card.getExpireDate();
            LocalDate date = LocalDate.of(1999, 12, 31);
            if (expireDate != null) {
            date = expireDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();}
            String cardHolderName = card.getHolderFirstname() + " " + card.getHolderLastname(); // will be null on almost all newer CCs

            String iban = card.getIban();
            String bic = card.getBic();
            String at = card.getAt();
            EmvCardScheme cardGetType = card.getType();
            if (cardGetType != null) {
                String typeName = card.getType().getName();
                String[] typeAids = card.getType().getAid();
                idContentString = idContentString + "\n" + "typeName: " + typeName;
                for (int i = 0; i < typeAids.length; i++) {
                    idContentString = idContentString + "\n" + "aid " + i + ": " + typeAids[i];
                }
            }

            List<Application> applications = card.getApplications();
            idContentString = idContentString + "\n" + "cardNumber: " + prettyPrintCardNumber(cardNumber);
            idContentString = idContentString + "\n" + "expireDate: " + date;
            idContentString = idContentString + "\n" + "cardholder name: " + cardHolderName;
            idContentString = idContentString + "\n" + "iban: " + iban + "bic: " + bic;
            idContentString = idContentString + "\n" + "at: " + at;

            idContentString = idContentString + "\n" + "applications:";
            for (int i = 0; i < applications.size(); i++) {
                Application apl = applications.get(i);
                String appData = "";
                appData = appData + "app nr: " + i + "\n";
                appData = appData + "aid: " + bytesToHex(apl.getAid())+ "\n";
                appData = appData + "amount: " + apl.getAmount() + "\n";
                appData = appData + "appLabel: " + apl.getApplicationLabel() + "\n";
                appData = appData + "priority: " + apl.getPriority() + "\n";
                appData = appData + "left pin try: " + apl.getLeftPinTry() + "\n";
                appData = appData + "transactionCounter: " + apl.getTransactionCounter() + "\n";
                List<EmvTransactionRecord> transactionsList = apl.getListTransactions();
                String transactionsListString = "";
                if (transactionsList != null) {
                    for (int j = 0; j < transactionsList.size(); j++) {
                        EmvTransactionRecord transaction = transactionsList.get(j);
                        Date transactionDate = transaction.getDate();
                        Float transactionAmount = transaction.getAmount();
                        LocalDate trDate = transactionDate.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        transactionsListString = transactionsListString + "\n" + "date: " + trDate + " amount: " + transactionAmount;
                    }
                    appData = appData + "tr-List: " + transactionsListString + "\n";
                }
                //appData = appData + "aid: " + apl.getAid() + "\n";

            idContentString = idContentString + "\n" + "app-data: " + appData;
            }
            EmvTrack1 emvTrack1 = card.getTrack1();
            if (emvTrack1 != null) {
                //String lastName = emvTrack1.getHolderLastname();
                String formatCode = emvTrack1.getFormatCode();
                Service service = emvTrack1.getService();
                String interchange = service.getServiceCode1().getInterchange();
                String technology = service.getServiceCode1().getTechnology();
                String authorizationProcessing = service.getServiceCode2().getAuthorizationProcessing();
                String allowedServices = service.getServiceCode3().getAllowedServices();
                String pinRequirements = service.getServiceCode3().getPinRequirements();
                idContentString = idContentString + "\n" + "emvTrack1 data " +
                        "lastName: " + "NULL" + " formatCode: " + formatCode + " interchange: " + interchange +
                        " technology: " + technology + " authorizationProcessing: " + authorizationProcessing +
                        " allowedServices: " + allowedServices + " pinRequirements: " + pinRequirements;
            }
            EmvTrack2 emvTrack2 = card.getTrack2();
            String track2CardNumber = "null";
            if (emvTrack2 != null) {
                track2CardNumber = emvTrack2.getCardNumber();
            }
            idContentString = idContentString + "\n" + "emvTrack2 data " +
            "track2CreditCard number + " + prettyPrintCardNumber(track2CardNumber);

            String finalIdContentString = idContentString;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText(finalIdContentString);
                }
            });

            try {
                isoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
        } catch (Exception e) {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();
        }

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public static String prettyPrintCardNumber(String cardNumber) {
        if (cardNumber == null) return null;
        char delimiter = ' ';
        return cardNumber.replaceAll(".{4}(?!$)", "$0" + delimiter);
    }

    // https://stackoverflow.com/a/51338700/8166854
    private byte[] selectApdu(byte[] aid) {
        byte[] commandApdu = new byte[6 + aid.length];
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xA4;  // INS
        commandApdu[2] = (byte) 0x04;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) (aid.length & 0x0FF);       // Lc
        System.arraycopy(aid, 0, commandApdu, 5, aid.length);
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }


    // source: https://stackoverflow.com/a/37047375/8166854 May 5, 2016 at 9:46
    // user Michael Roland
    boolean testCommand(NfcA nfcA, byte[] command) throws IOException {
        final boolean leaveConnected = nfcA.isConnected();

        boolean commandAvailable = false;

        if (!leaveConnected) {
            nfcA.connect();
        }

        try {
            byte[] result = nfcA.transceive(command);
            if ((result != null) &&
                    (result.length > 0) &&
                    !((result.length == 1) && ((result[0] & 0x00A) == 0x000))) {
                // some response received and response is not a NACK response
                commandAvailable = true;

                // You might also want to check if you received a response
                // that is plausible for the specific command before you
                // assume that the command is actualy available and what
                // you expected...
            }
        } catch (IOException e) {
            // IOException (including TagLostException) could indicate that
            // either the tag is no longer in range or that the command is
            // not supported by the tag
        }

        try {
            nfcA.close();
        } catch (Exception e) {
        }

        if (leaveConnected) {
            nfcA.connect();
        }

        return commandAvailable;
    }

    public static String shortToHex(short data) {
        return Integer.toHexString(data & 0xffff);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    /*
    public static byte[] BuildSelectApdu(String aid)
    {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return hexStringToByteArray(SELECT_APDU_HEADER + (aid.length() / 2).ToString("X2") + aid);
    }
*/
}
