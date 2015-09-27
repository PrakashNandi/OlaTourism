package model;

import java.util.ArrayList;

/**
 * Created by PrakashNandi on 27/09/15.
 */
public class CabAvailabilityDTO {

    public ArrayList<CategoriesDTO> categories;
    public RideEstimateDTO ride_estimate;

    public CabAvailabilityDTO(ArrayList<CategoriesDTO> categories, RideEstimateDTO ride_estimate) {
        this.categories = categories;
        this.ride_estimate = ride_estimate;
    }

    public ArrayList<CategoriesDTO> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<CategoriesDTO> categories) {
        this.categories = categories;
    }

    public RideEstimateDTO getRide_estimate() {
        return ride_estimate;
    }

    public void setRide_estimate(RideEstimateDTO ride_estimate) {
        this.ride_estimate = ride_estimate;
    }
}
