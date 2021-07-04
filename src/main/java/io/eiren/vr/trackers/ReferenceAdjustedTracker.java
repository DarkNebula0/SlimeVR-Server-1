package io.eiren.vr.trackers;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import io.eiren.vr.processor.TrackerBodyPosition;

public class ReferenceAdjustedTracker implements Tracker {
	
	public final Tracker tracker;
	private final Quaternion smoothedQuaternion = new Quaternion();
	public final Quaternion adjustmentYaw = new Quaternion();
	public final Quaternion adjustmentAttachment = new Quaternion();
	protected float[] lastAngles = new float[3];
	public float smooth = 0 * FastMath.DEG_TO_RAD;
	private final float[] angles = new float[3];
	protected float confidenceMultiplier = 1.0f;
	
	public ReferenceAdjustedTracker(Tracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public boolean userEditable() {
		return this.tracker.userEditable();
	}

	@Override
	public void loadConfig(TrackerConfig config) {
		this.tracker.loadConfig(config);
	}

	@Override
	public void saveConfig(TrackerConfig config) {
		this.tracker.saveConfig(config);
	}

	/**
	 *  Reset the tracker so that it's current rotation
	 *  is counted as (0, <HMD Yaw>, 0). This allows tracker
	 *  to be strapped to body at any pitch and roll.
	 *  <p>Performs {@link #resetYaw(Quaternion)} for yaw
	 *  drift correction.
	 */
	@Override
	public void resetFull(Quaternion reference) {
		resetYaw(reference);
		
		Quaternion sensorRotation = new Quaternion();
		tracker.getRotation(sensorRotation);
		adjustmentYaw.mult(sensorRotation, sensorRotation);

		// Use only yaw HMD rotation
		Quaternion targetTrackerRotation = new Quaternion(reference);
		float[] angles = new float[3];
		targetTrackerRotation.toAngles(angles);
		targetTrackerRotation.fromAngles(0, angles[1], 0);
		
		adjustmentAttachment.set(sensorRotation).inverseLocal().multLocal(targetTrackerRotation);
	}

	/**
	 *  Reset the tracker so that it's current yaw rotation
	 *  is counted as <HMD Yaw>. This allows the tracker
	 *  to have yaw independant of the HMD. Tracker should
	 *  still report yaw as if it was mounted facing HMD,
	 *  mounting position should be corrected in the source.
	 */
	@Override
	public void resetYaw(Quaternion reference) {
		// Use only yaw HMD rotation
		Quaternion targetTrackerRotation = new Quaternion(reference);
		float[] angles = new float[3];
		targetTrackerRotation.toAngles(angles);
		targetTrackerRotation.fromAngles(0, angles[1], 0);
		
		Quaternion sensorRotation = new Quaternion();
		tracker.getRotation(sensorRotation);
		
		sensorRotation.toAngles(angles);
		sensorRotation.fromAngles(0, angles[1], 0);
		
		adjustmentYaw.set(sensorRotation).inverseLocal().multLocal(targetTrackerRotation);
		
		confidenceMultiplier = 1.0f / tracker.getConfidenceLevel();
		lastAngles[0] = 1000;
	}
	
	protected void adjustInternal(Quaternion store) {
		store.multLocal(adjustmentAttachment);
		adjustmentYaw.mult(store, store);
	}
	
	@Override
	public boolean getRotation(Quaternion store) {
		tracker.getRotation(store);
		if(smooth > 0) {
			store.toAngles(angles);
			if(Math.abs(angles[0] - lastAngles[0]) > smooth || Math.abs(angles[1] - lastAngles[1]) > smooth || Math.abs(angles[2] - lastAngles[2]) > smooth) {
				smoothedQuaternion.set(store);
				store.toAngles(lastAngles);
			} else {
				store.set(smoothedQuaternion);
			}
		}
		adjustInternal(store);
		return true;
	}

	@Override
	public boolean getPosition(Vector3f store) {
		return tracker.getPosition(store);
	}

	@Override
	public String getName() {
		return tracker.getName() + "/adj";
	}

	@Override
	public TrackerStatus getStatus() {
		return tracker.getStatus();
	}
	
	@Override
	public float getConfidenceLevel() {
		return tracker.getConfidenceLevel() * confidenceMultiplier;
	}

	@Override
	public TrackerBodyPosition getBodyPosition() {
		return tracker.getBodyPosition();
	}

	@Override
	public void setBodyPosition(TrackerBodyPosition position) {
		tracker.setBodyPosition(position);
	}
}