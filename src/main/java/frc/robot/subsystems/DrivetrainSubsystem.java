// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

//import com.ctre.phoenix.sensors.PigeonIMU;
import com.kauailabs.navx.frc.AHRS;

import swervelib.SwerveModule;
import swervelib.config.Mk3SwerveModuleHelper;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.*;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import frc.robot.RobotContainer;
import frc.robot.utils.*;
import frc.robot.utils.math.InterpolatingTreeMap;
import frc.robot.utils.math.MathUtils;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer;

import static frc.robot.Constants.*;

/*
// This can help when dealing with pathfinding
https://github.com/acmerobotics/road-runner-quickstart
*/

public class DrivetrainSubsystem extends BaseSubsystem {

  private final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
      // Front left
      new Translation2d(TRACKWIDTH_METERS / 2.0, WHEELBASE_METERS / 2.0),
      // Front right
      new Translation2d(TRACKWIDTH_METERS / 2.0, -WHEELBASE_METERS / 2.0),
      // Back left
      new Translation2d(-TRACKWIDTH_METERS / 2.0, WHEELBASE_METERS / 2.0),
      // Back right
      new Translation2d(-TRACKWIDTH_METERS / 2.0, -WHEELBASE_METERS / 2.0));
  private static final int MAX_LATENCY_COMPENSATION_MAP_ENTRIES = 25;

  private final AHRS m_gyro;

  private final InterpolatingTreeMap<Pose2d> m_lagCompensationMap = InterpolatingTreeMap
      .createBuffer(MAX_LATENCY_COMPENSATION_MAP_ENTRIES);

  public static ShuffleboardTab m_DrivetrainTab, m_DriveDataTab;
  private final NetworkTableEntry m_isFieldOriented;
  private static ShuffleboardLayout m_OdometryData, m_ChassisData, m_OtherData;
  // These are our modules. We initialize them in the constructor.
  private final SwerveModule m_frontLeftModule, m_frontRightModule, m_backLeftModule, m_backRightModule;

  private SimpleMotorFeedforward m_feedForward = new SimpleMotorFeedforward(Auto.STATIC_GAIN, Auto.VELOCITY_GAIN,
      Auto.ACCELERATION_GAIN);

  private ChassisSpeeds m_chassisSpeeds = new ChassisSpeeds(0.0, 0.0, 0.0);
  private final SwerveDriveOdometry m_odometry = new SwerveDriveOdometry(m_kinematics, new Rotation2d(0));

  private double m_lastWorldAccelX = -1.0, m_lastWorldAccelY = -1.0;
  private boolean m_didCollide = false;
  private double m_lastCollisionTime = 0.0;

  private double m_speedModifier = 1.0;

  public DrivetrainSubsystem() {
    m_gyro = new AHRS(SPI.Port.kMXP, (byte) 200); // NavX connected over MXP
    if (m_gyro.isBoardlevelYawResetEnabled()) {
      m_gyro.enableBoardlevelYawReset(false);
    }

    resetPose(new Pose2d(7, 2, Rotation2d.fromDegrees(-90)));

    m_DrivetrainTab = addTab("Drivetrain");
    m_DriveDataTab = addTab("Drive Data");
    m_frontLeftModule = Mk3SwerveModuleHelper.createFalcon500(
        // This parameter is optional, but will allow you to see the current state of
        // the module on the dashboard.
        m_DrivetrainTab.getLayout("Front Left Module", BuiltInLayouts.kList)
            .withSize(2, 2)
            .withPosition(0, 0),
        // This can either be STANDARD or FAST depending on your gear configuration
        Mk3SwerveModuleHelper.GearRatio.STANDARD,
        Ids.FRONT_LEFT.DRIVE_MOTOR,
        Ids.FRONT_LEFT.STEER_MOTOR,
        Ids.FRONT_LEFT.STEER_ENCODER,
        Ids.FRONT_LEFT.STEER_OFFSET);

    // We will do the same for the other modules
    m_frontRightModule = Mk3SwerveModuleHelper.createFalcon500(
        m_DrivetrainTab.getLayout("Front Right Module", BuiltInLayouts.kList)
            .withSize(2, 2)
            .withPosition(2, 0),
        Mk3SwerveModuleHelper.GearRatio.STANDARD,
        Ids.FRONT_RIGHT.DRIVE_MOTOR,
        Ids.FRONT_RIGHT.STEER_MOTOR,
        Ids.FRONT_RIGHT.STEER_ENCODER,
        Ids.FRONT_RIGHT.STEER_OFFSET);

    m_backLeftModule = Mk3SwerveModuleHelper.createFalcon500(
        m_DrivetrainTab.getLayout("Back Left Module", BuiltInLayouts.kList)
            .withSize(2, 2)
            .withPosition(4, 0),
        Mk3SwerveModuleHelper.GearRatio.STANDARD,
        Ids.BACK_LEFT.DRIVE_MOTOR,
        Ids.BACK_LEFT.STEER_MOTOR,
        Ids.BACK_LEFT.STEER_ENCODER,
        Ids.BACK_LEFT.STEER_OFFSET);

    m_backRightModule = Mk3SwerveModuleHelper.createFalcon500(
        m_DrivetrainTab.getLayout("Back Right Module", BuiltInLayouts.kList)
            .withSize(2, 2)
            .withPosition(6, 0),
        Mk3SwerveModuleHelper.GearRatio.STANDARD,
        Ids.BACK_RIGHT.DRIVE_MOTOR,
        Ids.BACK_RIGHT.STEER_MOTOR,
        Ids.BACK_RIGHT.STEER_ENCODER,
        Ids.BACK_RIGHT.STEER_OFFSET);

    zeroGyro();
    m_OdometryData = m_DriveDataTab.getLayout("Odometry Data", BuiltInLayouts.kList)
        .withSize(2, 2)
        .withPosition(0, 0);
    m_ChassisData = m_DriveDataTab.getLayout("Chassis Data", BuiltInLayouts.kList)
        .withSize(2, 2)
        .withPosition(2, 0);
    m_OtherData = m_DriveDataTab.getLayout("Other Data", BuiltInLayouts.kList)
        .withSize(2, 3)
        .withPosition(4, 0);
    m_OdometryData.addNumber("X", () -> {
      return getPose().getTranslation().getX();
    });
    m_OdometryData.addNumber("Y", () -> {
      return getPose().getTranslation().getY();
    });
    m_OdometryData.addNumber("Angle", () -> {
      return getPose().getRotation().getDegrees();
    });

    m_ChassisData.addNumber("X", () -> {
      return m_chassisSpeeds.vxMetersPerSecond;
    });
    m_ChassisData.addNumber("Y", () -> {
      return m_chassisSpeeds.vyMetersPerSecond;
    });
    m_ChassisData.addNumber("Z", () -> {
      return m_chassisSpeeds.omegaRadiansPerSecond;
    });

    m_isFieldOriented = m_OtherData.add("Field Oriented", true).getEntry();
    m_OtherData.addNumber("Gyro Rotation", () -> {
      return getGyroRotation().getDegrees();
    });
  }

  /**
   * Sets the gyroscope angle to zero. This can be used to set the direction the
   * robot is currently facing to the
   * 'forwards' direction.
   */
  public void zeroGyro() {
    m_gyro.reset();
  }

  public Rotation2d getGyroRotation() {
    // We have to invert the angle of the NavX so that rotating the robot
    // counter-clockwise makes the angle increase.

    // if (ENABLE_MAGNETOMETER && m_navx.isMagnetometerCalibrated()) {
    // return Rotation2d.fromDegrees(m_navx.getFusedHeading());
    // }
    return Rotation2d.fromDegrees((INVERT_GYRO ? 360.0 : 0) - m_gyro.getYaw());
  }

  public boolean getDidCollide() {
    double curWorldAccelX = m_gyro.getWorldLinearAccelX();
    double curWorldAccelY = m_gyro.getWorldLinearAccelY();

    double curJerkX = curWorldAccelX - m_lastWorldAccelX;
    double curJerkY = curWorldAccelY - m_lastWorldAccelY;

    m_lastWorldAccelX = curWorldAccelX;
    m_lastWorldAccelY = curWorldAccelY;

    if ((Math.abs(curJerkX) > COLLISION_THRESHOLD_DELTA) ||
        (Math.abs(curJerkY) > COLLISION_THRESHOLD_DELTA)) {
      m_didCollide = true;
      m_lastCollisionTime = Timer.getFPGATimestamp() + 5.0;
    } else if (Timer.getFPGATimestamp() > m_lastCollisionTime) {
      m_didCollide = false;
    }
    return m_didCollide;
  }

  public void driveWithHeading(double translation_x, double translation_y, double headingDegrees) {

    double angle = getGyroRotation().getDegrees();
    double currentAngularRate = -getTurnRate();
    double angle_error = MathUtils.angleDelta(headingDegrees, angle);
    double yawCommand = -angle_error * Auto.PID_THETA.P - (currentAngularRate);

    drive(translation_x, translation_y, Math.toRadians(yawCommand), true);
  }

  public void drive(double translation_x, double translation_y, double rotation) {
    m_chassisSpeeds = m_isFieldOriented.getBoolean(true)
        ? ChassisSpeeds.fromFieldRelativeSpeeds(translation_x, translation_y, rotation, getGyroRotation())
        : new ChassisSpeeds(translation_x, translation_y, rotation);
  }

  public void drive(double translation_x, double translation_y, double rotation, boolean fieldOriented) {
    m_chassisSpeeds = fieldOriented
        ? ChassisSpeeds.fromFieldRelativeSpeeds(translation_x, translation_y, rotation, getGyroRotation())
        : new ChassisSpeeds(translation_x, translation_y, rotation);
  }

  // Calcs from gains to drive accuratly
  // Ex: Drive 1 meter with drive fails where this would not
  private double getVelocityToVoltage(double speedMetersPerSecond) {
    double wheel_voltage = Motor.MAX_POWER.getDouble(Motor.DEFAULT_MAX_POWER);
    if (Motor.ENABLE_FF)
      return speedMetersPerSecond / Motor.MAX_VELOCITY_MPS * wheel_voltage;
    return MathUtils.clamp(m_feedForward.calculate(speedMetersPerSecond), -wheel_voltage, wheel_voltage);
  }

  public double getTurnRate() {
    return m_gyro.getRate();
  }

  public SwerveDriveKinematics getKinematics() {
    return m_kinematics;
  }

  public Pose2d getPose() {
    return m_odometry.getPoseMeters();
  }

  public void resetPose(Pose2d pose) {
    m_odometry.resetPosition(pose, getGyroRotation());
  }

  public void updateOdometry(SwerveModuleState[] states) {
    m_odometry.update(getGyroRotation(), states); // Update Pose

    if (m_lagCompensationMap.size() > MAX_LATENCY_COMPENSATION_MAP_ENTRIES) {
      m_lagCompensationMap.getSample(m_lagCompensationMap.firstKey());
    }
    m_lagCompensationMap.addSample(Timer.getFPGATimestamp(), m_odometry.getPoseMeters()); // Add to interp map
  }

  public Pose2d getLagCompPose(double timestamp) {
    if (m_lagCompensationMap.isEmpty()) {
      return new Pose2d();
    }
    return m_lagCompensationMap.getSample(timestamp);
  }

  public void setSpeedModifier(double mod) {
    m_speedModifier = mod;
  }

  public void setModuleStates(SwerveModuleState[] states) {
    m_chassisSpeeds = m_kinematics.toChassisSpeeds(states);
  }

  public void setModuleStates(ChassisSpeeds state) {
    m_chassisSpeeds = state;
  }

  public void stopModules() {
    m_chassisSpeeds = new ChassisSpeeds(0, 0, 0);
  }

  // This is basic drift correction
  // I added it as it might be usefull later
  // Currently not used
  private static PIDController m_driftCorrectionPID = new PIDController(0.07, 0.00, 0.004);
  private static double m_desiredHeading;
  private static double m_prevXY = 0;

  public void getDriftCorrection(ChassisSpeeds speeds) {

    double curXY = Math.abs(speeds.vxMetersPerSecond) + Math.abs(speeds.vyMetersPerSecond);

    if (Math.abs(speeds.omegaRadiansPerSecond) > 0.0 || m_prevXY <= 0)
      m_desiredHeading = getPose().getRotation().getDegrees();

    else if (curXY > 0)
      speeds.omegaRadiansPerSecond += m_driftCorrectionPID.calculate(getPose().getRotation().getDegrees(),
          m_desiredHeading);

    m_prevXY = curXY;
  }

  @Override
  public void periodic() {
    if (!IsSim()) {
      updateOdometry(getSwerveModuleStates()); // Update odometry based off wheel states, NOT requested chassis speeds
    }
    if (Math.abs(m_chassisSpeeds.vxMetersPerSecond) == 0
        && Math.abs(m_chassisSpeeds.vyMetersPerSecond) == 0
        && Math.abs(m_chassisSpeeds.omegaRadiansPerSecond) == 0
        && Motor.DEFENSIVE) {
      m_frontLeftModule.set(
          0.0, Math.toRadians(-45));
      m_frontRightModule.set(
          0.0, Math.toRadians(45));
      m_backLeftModule.set(
          0.0, Math.toRadians(45));
      m_backRightModule.set(
          0.0, Math.toRadians(-45));
    } else {

      // Speed Motifier
      m_chassisSpeeds.vxMetersPerSecond *= m_speedModifier;
      m_chassisSpeeds.vyMetersPerSecond *= m_speedModifier;
      m_chassisSpeeds.omegaRadiansPerSecond *= m_speedModifier;

      SwerveModuleState[] states = m_kinematics.toSwerveModuleStates(m_chassisSpeeds);
      SwerveDriveKinematics.desaturateWheelSpeeds(states, Motor.MAX_VELOCITY_MPS);

      m_frontLeftModule.set(getVelocityToVoltage(
          states[0].speedMetersPerSecond),
          states[0].angle.getRadians());
      m_frontRightModule.set(getVelocityToVoltage(
          states[1].speedMetersPerSecond),
          states[1].angle.getRadians());
      m_backLeftModule.set(getVelocityToVoltage(
          states[2].speedMetersPerSecond),
          states[2].angle.getRadians());
      m_backRightModule.set(getVelocityToVoltage(
          states[3].speedMetersPerSecond),
          states[3].angle.getRadians());
      if (IsSim()) {
        updateOdometry(states); // Sim doesnt deal with offsets ex: offsets break odometry
      }
    }
    RobotContainer.dashboardField.setRobotPose(m_odometry.getPoseMeters()); // set field pose
  }

  public SwerveModule[] getSwerveModules() {
    return new SwerveModule[] { m_frontLeftModule, m_frontRightModule, m_backLeftModule, m_backRightModule };
  }

  public SwerveModuleState[] getSwerveModuleStates() {
    return new SwerveModuleState[] {
        m_frontLeftModule.getState(),
        m_frontRightModule.getState(),
        m_backLeftModule.getState(),
        m_backRightModule.getState()
    };
  }
}
