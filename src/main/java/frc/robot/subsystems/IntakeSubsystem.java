package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.Arm;
import frc.robot.Constants.Ids;
import frc.robot.Constants.Intake;
import frc.robot.utils.Controller.XboxControllerExtended;
import oblog.annotations.Config;

public class IntakeSubsystem extends SubsystemBase {

  private final WPI_TalonFX m_intake = new WPI_TalonFX(Ids.INTAKE);
  private final WPI_TalonFX m_arm = new WPI_TalonFX(Ids.ARM);
  private final XboxControllerExtended m_controller;

  private final double m_armTimeUp = 0.7;
  private final double m_armTimeDown = 0.3;

  // TODO: Add zeroing
  // 38991 when up
  // 1555 when down

  // true == up false == down
  private boolean m_armState = true;
  private boolean m_verifiedArmState = true;
  private double m_lastBurstTime = 0;
  private ShuffleboardTab m_intakeTab;

  public IntakeSubsystem(XboxControllerExtended controller) {
    m_controller = controller;
    m_arm.setInverted(false);
    m_arm.setNeutralMode(NeutralMode.Brake); // Force Motor in brake mode

    m_armState = updateArmState();

    m_intakeTab = Shuffleboard.getTab("IntakeSubsystem");
    m_intakeTab.addBoolean("Arm State", () -> m_armState);
    m_intakeTab.addBoolean("Verified Arm State", () -> m_verifiedArmState);
    m_intakeTab.addNumber("Arm Selected Position", () -> m_arm.getSelectedSensorPosition());
  }

  public boolean isOkay() {

    return true;
  }

  public boolean updateArmState() {
    if (m_arm.getSelectedSensorPosition() > Arm.UP_POSITION) {
      m_verifiedArmState = true;
    } else if (m_arm.getSelectedSensorPosition() > Arm.DOWN_POSITION) {
      m_verifiedArmState = false;
    }
    return m_verifiedArmState;
  }

  public void updateIntake() {
    if (m_controller.getLeftBumper()) {
      m_intake.set(ControlMode.PercentOutput, Intake.POWER);
    } else if (m_controller.getRightBumper()) {
      m_intake.set(ControlMode.PercentOutput, -Intake.POWER);
    } else {
      m_intake.set(ControlMode.PercentOutput, 0);
    }
  }

  public void updateArm() {
    if (m_armState) {
      if (Timer.getFPGATimestamp() - m_lastBurstTime < m_armTimeUp) {
        m_arm.set(Arm.TRAVEL_UP_POWER);
      } else if (m_arm.getSelectedSensorPosition() < Arm.UP_POSITION) {
        m_arm.set(Arm.TRAVEL_DIFFRENCE);
      } else {
        m_arm.set(0);
      }
    } else {
      if (Timer.getFPGATimestamp() - m_lastBurstTime < m_armTimeDown) {
        m_arm.set(-Arm.TRAVEL_DOWN_POWER);
      } else if (m_arm.getSelectedSensorPosition() > Arm.DOWN_POSITION) {
        m_arm.set(-Arm.TRAVEL_DIFFRENCE);
      } else {
        m_arm.set(0);
      }
    }

    if (m_controller.getRightTriggerAxis() > 0 && !m_armState) {
      m_lastBurstTime = Timer.getFPGATimestamp();
      m_armState = true;
    } else if (m_controller.getLeftTriggerAxis() > 0 && m_armState) {
      m_lastBurstTime = Timer.getFPGATimestamp();
      m_armState = false;
    }
  }

  @Override
  public void periodic() {
    updateArmState();
    updateIntake();
    updateArm();
  }
}