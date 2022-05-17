package de.androidcrypto.androidemvreaderown;

import android.nfc.tech.IsoDep;
import android.util.Log;

import com.github.devnied.emvnfccard.enums.SwEnum;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.utils.TlvUtil;


import java.io.IOException;
import java.security.Provider;

import fr.devnied.bitlib.BytesUtils;

public class PcscProvider implements IProvider {
    // source: https://stackoverflow.com/questions/58825020/kotlin-emv-android-nfc-tag-setconnectedtechnology-error
    private static final String TAG = "Provider";


    private IsoDep mTagCom;

    public void setmTagCom(final IsoDep mTagCom) {
        this.mTagCom = mTagCom;
    }

    @Override
    public byte[] transceive(byte[] pCommand) throws CommunicationException {


        byte[] response = null;
        try {
            // send command to emv card
            mTagCom.getTag();
            //mTagCom.connect();
            if (mTagCom.isConnected()){
                response = mTagCom.transceive(pCommand);
            }
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage());
        }


        Log.d(TAG, "resp: " + BytesUtils.bytesToString(response));
        try {
            Log.d(TAG, "resp: " + TlvUtil.prettyPrintAPDUResponse(response));
            SwEnum val = SwEnum.getSW(response);
            if (val != null) {
                Log.d(TAG, "resp: " + val.getDetail());
            }
        } catch (Exception e) {
        }

        return response;
    }


    @Override
    public byte[] getAt() {
        // return new byte[0]; // from Stackoverflow
        return mTagCom.getHistoricalBytes();
    }
}