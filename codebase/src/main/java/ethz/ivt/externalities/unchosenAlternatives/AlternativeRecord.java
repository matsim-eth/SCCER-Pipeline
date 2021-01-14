package ethz.ivt.externalities.unchosenAlternatives;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import com.opencsv.bean.CsvNumber;

import java.time.LocalDateTime;

public class AlternativeRecord {

	@CsvBindByName(column="user_id", required=true)
	String userId;
	
	@CsvBindByName(column="trip_id", required=true)
	String tripId;

	@CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	@CsvBindByName(required=true)
	LocalDateTime started_at;

	@CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	@CsvBindByName(required=true)
	LocalDateTime finished_at;
	
	
	@CsvBindByName(column="mode", required=true)
	String mode;

	@CsvBindByName(required=true)
	double start_x;
	
	@CsvBindByName(required=true)
	double start_y;
	
	@CsvBindByName(required=true)
	double end_x;
	
	@CsvBindByName(required=true)
	double end_y;
	
	@CsvBindByName(required=true)
	@CsvNumber("#.##")
	double euclideanDistance;
	
	@CsvBindByName(required=true)
	@CsvNumber("#.##")
	double distance;
	
	@CsvBindByName(required=true)
	@CsvNumber("#.##")
	double totalTravelTime;
	
	@CsvBindByName
	@CsvNumber("#.##")
	Double cost;

	@CsvBindByName
	Double frequency;
	
	@CsvBindByName
	Integer transfers;
	
	@CsvBindByName
	private boolean chosen = false;

	@CsvBindByName
	@CsvNumber("#.##")
	private double inTrainVehicleTime;

	@CsvBindByName
	@CsvNumber("#.##")
	private double inTrainVehicleDistance;

	@CsvBindByName
	@CsvNumber("#.##")
	private double localTransitInVehicleTime;

	@CsvBindByName
	@CsvNumber("#.##")
	private double localTransitInVehicleDistance;

	@CsvBindByName
	private boolean isTrainJourney;
	
	@CsvBindByName
	private boolean tripMadeWithHalbtax;

	private boolean isValid = true;

	public AlternativeRecord() {
	}

	public String getUserId() {
		return userId;
	}

	public String getTripId() {
		return tripId;
	}

	public LocalDateTime getStarted_at() {
		return started_at;
	}

	public LocalDateTime getFinished_at() {
		return finished_at;
	}

	public double getStart_x() {
		return start_x;
	}

	public double getStart_y() {
		return start_y;
	}

	public double getEnd_x() {
		return end_x;
	}

	public double getEnd_y() {
		return end_y;
	}

	public boolean isValidAlternative() {
		return isValid;
	}
	
	public boolean isChosenAlternative() {
		return chosen;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public double getTotalTravelTime() {
		return totalTravelTime;
	}

	public double getDistance() {
		return distance;
	}

	public double getEuclideanDistance() {
		return euclideanDistance;
	}

	public boolean isTrainJourney() {
		return isTrainJourney;
	}

}
