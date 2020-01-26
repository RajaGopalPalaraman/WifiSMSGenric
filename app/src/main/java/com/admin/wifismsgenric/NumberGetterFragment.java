package com.admin.wifismsgenric;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Set;

public class NumberGetterFragment extends Fragment {

    private static final String PHONE_NUMBER_REGEX = "^\\+\\d{7,}";

    private static final String LOCAL_PERSISTENCE_NAME = "phoneNumberFile";
    private static final String LOCAL_PERSISTENCE_KEY = "phoneNumbersKey";

    private static final int PERMISSION_REQUEST_CODE = 89;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.number_fetcher_layout,container,false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Set<String> numbers = getPersistedNumbers();
        if (numbers != null && !numbers.isEmpty()) {
            String[] numbersArray = numbers.toArray(new String[0]);
            switch (numbersArray.length)
            {
                case 2:
                    ((EditText)getActivity().findViewById(R.id.number2)).setText(numbersArray[1]);
                case 1:
                    ((EditText)getActivity().findViewById(R.id.number1)).setText(numbersArray[0]);
            }
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS
                    }, PERMISSION_REQUEST_CODE);
        }

        getActivity().findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number1 = ((EditText)getActivity().findViewById(R.id.number1)).getText()
                        .toString().trim();
                String number2 = ((EditText)getActivity().findViewById(R.id.number2)).getText()
                        .toString().trim();

                Set<String> hashSet = new HashSet<>();
                if (number1.isEmpty() && number2.isEmpty())
                {
                    Toast.makeText(getContext(),R.string.numberRequired,Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (number2.isEmpty())
                {
                    if (number1.matches(PHONE_NUMBER_REGEX)) {
                        hashSet.add(number1);
                    }
                    else
                    {
                        Toast.makeText(getContext(),R.string.enterValidNumber,Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                else if (number1.isEmpty())
                {
                    if (number2.matches(PHONE_NUMBER_REGEX)) {
                        hashSet.add(number2);
                    }
                    else
                    {
                        Toast.makeText(getContext(),R.string.enterValidNumber,Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                else
                {
                    if (number1.matches(PHONE_NUMBER_REGEX)
                            && number2.matches(PHONE_NUMBER_REGEX) ) {
                        hashSet.add(number1);
                        hashSet.add(number2);
                    }
                    else
                    {
                        Toast.makeText(getContext(),R.string.enterValidNumber,Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                persistNumbers(hashSet);
                Bundle bundle = new Bundle();
                bundle.putStringArray(DashboardFragment.NUMBERS_ARRAY, hashSet.toArray(new String[0]));

                Fragment fragment = new DashboardFragment();
                fragment.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.root_layout,fragment).commit();
            }
        });
    }

    private void persistNumbers(@Nullable Set<String> phoneNumbers)
    {
        getContext().getSharedPreferences(LOCAL_PERSISTENCE_NAME, Context.MODE_PRIVATE).edit()
                    .putStringSet(LOCAL_PERSISTENCE_KEY,phoneNumbers).apply();
    }

    @Nullable
    private Set<String> getPersistedNumbers()
    {
        return getContext().getSharedPreferences(LOCAL_PERSISTENCE_NAME, Context.MODE_PRIVATE).
                getStringSet(LOCAL_PERSISTENCE_KEY,null);

    }
}
