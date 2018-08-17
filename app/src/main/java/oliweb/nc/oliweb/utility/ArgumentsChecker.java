package oliweb.nc.oliweb.utility;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.EnumMap;
import java.util.Map;

import io.reactivex.functions.Predicate;

public class ArgumentsChecker {

    private static final String TAG = ArgumentsChecker.class.getCanonicalName();

    private enum TypeCheck {
        OPTIONAL,
        MANDATORY
    }

    private Bundle arguments;
    private Map<TypeCheck, Pair<String, Predicate<Bundle>>> listArgCheck;
    private OnSuccessListener<Bundle> successListener;
    private OnFailureListener onFailureListener;

    public ArgumentsChecker setArguments(Bundle args) {
        this.arguments = args;
        return this;
    }

    public ArgumentsChecker setOnSuccessListener(OnSuccessListener<Bundle> successListener) {
        this.successListener = successListener;
        return this;
    }

    public ArgumentsChecker setOnFailureListener(OnFailureListener onFailureListener) {
        this.onFailureListener = onFailureListener;
        return this;
    }

    private Map<TypeCheck, Pair<String, Predicate<Bundle>>> getListArgCheck() {
        if (listArgCheck == null) {
            listArgCheck = new EnumMap<>(TypeCheck.class);
        }
        return listArgCheck;
    }

    public ArgumentsChecker isMandatory(String argName) {
        getListArgCheck().put(TypeCheck.MANDATORY, new Pair<>(argName, bundle -> true));
        return this;
    }

    public ArgumentsChecker isOptional(String argName) {
        getListArgCheck().put(TypeCheck.OPTIONAL, new Pair<>(argName, bundle -> true));
        return this;
    }

    public ArgumentsChecker isMandatoryWithCondition(String argName, Predicate<Bundle> predicate) {
        getListArgCheck().put(TypeCheck.MANDATORY, new Pair<>(argName, predicate));
        return this;
    }

    public ArgumentsChecker isOptionalWithCondition(String argName, Predicate<Bundle> predicate) {
        getListArgCheck().put(TypeCheck.OPTIONAL, new Pair<>(argName, predicate));
        return this;
    }

    public boolean check() {

        if (arguments == null || arguments.isEmpty()) {
            Log.e(TAG, "Arguments bundle are null or empty");

            if (onFailureListener != null) {
                onFailureListener.onFailure(new RuntimeException("Arguments bundle are null or empty"));
            }

            return false;
        }

        if (listArgCheck == null || listArgCheck.isEmpty()) {
            Log.w(TAG, "No list of arguments to check. You should use isMandatory() or isOptional to add arguments for checking.");

            if (successListener != null) {
                successListener.onSuccess(arguments);
            }

            return true;
        }

        for (Map.Entry<TypeCheck, Pair<String, Predicate<Bundle>>> entry : listArgCheck.entrySet()) {
            Pair<String, Predicate<Bundle>> pair = entry.getValue();
            String argName = pair.first;
            Predicate<Bundle> predicate = pair.second;
            TypeCheck typeCheck = entry.getKey();
            try {
                if ((predicate != null && predicate.test(arguments)) && (!arguments.containsKey(argName) || arguments.get(argName) == null)) {
                    if (typeCheck == TypeCheck.OPTIONAL) {
                        Log.w(TAG, String.format("Argument named %s is optional but is null or non present", argName));
                    } else {
                        Log.e(TAG, String.format("Argument named %s is mandatory but is null or non present", argName));

                        if (onFailureListener != null) {
                            onFailureListener.onFailure(new RuntimeException("Arguments not matching requirements"));
                        }

                        return false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }

        if (successListener != null) {
            successListener.onSuccess(arguments);
        }

        return true;
    }
}
