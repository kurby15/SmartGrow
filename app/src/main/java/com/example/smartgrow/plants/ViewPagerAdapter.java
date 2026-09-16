package com.example.smartgrow.plants;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new PlantInfoFragment();     // fragment_plant_info.xml
            case 1: return new ScheduleFragment();      // fragment_plant_schedule.xml
            case 2: return new HistoryFragment();       // fragment_plant_history.xml
            default: return new PlantInfoFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // May tatlong tabs ka
    }
}