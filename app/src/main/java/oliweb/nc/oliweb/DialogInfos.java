package oliweb.nc.oliweb;

import android.os.Bundle;

/**
 * Created by orlanth23 on 04/03/2018.
 */

public class DialogInfos {
    private String message;
    private int buttonType;
    private int idDrawable;
    private String tag;
    private Bundle bundlePar;

    public String getMessage() {
        return message;
    }

    public DialogInfos setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getButtonType() {
        return buttonType;
    }

    public DialogInfos setButtonType(int buttonType) {
        this.buttonType = buttonType;
        return this;
    }

    public int getIdDrawable() {
        return idDrawable;
    }

    public DialogInfos setIdDrawable(int idDrawable) {
        this.idDrawable = idDrawable;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public DialogInfos setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public Bundle getBundlePar() {
        return bundlePar;
    }

    public DialogInfos setBundlePar(Bundle bundlePar) {
        this.bundlePar = bundlePar;
        return this;
    }
}
