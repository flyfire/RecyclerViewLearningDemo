/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.fragment.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.solarexsoft.fragment.R;

class FragmentLayoutInflaterFactory implements LayoutInflater.Factory2 {
    private static final String TAG = FragmentManager.TAG;

    private final FragmentManager mFragmentManager;

    FragmentLayoutInflaterFactory(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        if (FragmentContainerView.class.getName().equals(name)) {
            return new FragmentContainerView(context, attrs, mFragmentManager);
        }

        if (!"fragment".equals(name)) {
            return null;
        }

        String fname = attrs.getAttributeValue(null, "class");
        TypedArray a =  context.obtainStyledAttributes(attrs, R.styleable.Fragment);
        if (fname == null) {
            fname = a.getString(R.styleable.Fragment_android_name);
        }
        int id = a.getResourceId(R.styleable.Fragment_android_id, View.NO_ID);
        String tag = a.getString(R.styleable.Fragment_android_tag);
        a.recycle();

        if (fname == null || !FragmentFactory.isFragmentClass(context.getClassLoader(), fname)) {
            // Invalid support lib fragment; let the device's framework handle it.
            // This will allow android.app.Fragments to do the right thing.
            return null;
        }

        int containerId = parent != null ? parent.getId() : 0;
        if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Must specify unique android:id, android:tag, or "
                    + "have a parent with an id for " + fname);
        }

        // If we restored from a previous state, we may already have
        // instantiated this fragment from the state and should use
        // that instance instead of making a new one.
        Fragment fragment = id != View.NO_ID ? mFragmentManager.findFragmentById(id) : null;
        if (fragment == null && tag != null) {
            fragment = mFragmentManager.findFragmentByTag(tag);
        }
        if (fragment == null && containerId != View.NO_ID) {
            fragment = mFragmentManager.findFragmentById(containerId);
        }

        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "onCreateView: id=0x"
                    + Integer.toHexString(id) + " fname=" + fname
                    + " existing=" + fragment);
        }
        FragmentStateManager fragmentStateManager;
        if (fragment == null) {
            fragment = mFragmentManager.getFragmentFactory().instantiate(
                    context.getClassLoader(), fname);
            fragment.mFromLayout = true;
            fragment.mFragmentId = id != 0 ? id : containerId;
            fragment.mContainerId = containerId;
            fragment.mTag = tag;
            fragment.mInLayout = true;
            fragment.mFragmentManager = mFragmentManager;
            fragment.mHost = mFragmentManager.getHost();
            fragment.onInflate(mFragmentManager.getHost().getContext(), attrs,
                    fragment.mSavedFragmentState);
            fragmentStateManager = mFragmentManager.createOrGetFragmentStateManager(fragment);
            mFragmentManager.addFragment(fragment);

        } else if (fragment.mInLayout) {
            // A fragment already exists and it is not one we restored from
            // previous state.
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Duplicate id 0x" + Integer.toHexString(id)
                    + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId)
                    + " with another fragment for " + fname);
        } else {
            // This fragment was retained from a previous instance; get it
            // going now.
            fragment.mInLayout = true;
            fragment.mFragmentManager = mFragmentManager;
            fragment.mHost = mFragmentManager.getHost();
            // Give the Fragment the attributes to initialize itself.
            fragment.onInflate(mFragmentManager.getHost().getContext(), attrs,
                    fragment.mSavedFragmentState);
            fragmentStateManager = mFragmentManager.createOrGetFragmentStateManager(fragment);
        }

        // The <fragment> tag is the one case where we:
        // 1) Move the Fragment to CREATED even if the FragmentManager isn't yet CREATED
        fragmentStateManager.moveToExpectedState();
        // 2) Create the Fragment's view despite not always moving to ACTIVITY_CREATED
        fragmentStateManager.ensureInflatedView();

        if (fragment.mView == null) {
            throw new IllegalStateException("Fragment " + fname
                    + " did not create a view.");
        }
        if (id != 0) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        return fragment.mView;
    }
}
