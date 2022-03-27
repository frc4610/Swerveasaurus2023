package frc.robot.subsystems;

import java.util.Optional;

import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.RobotContainer;
import frc.robot.Constants.*;
import frc.robot.commands.Autonomous.AutoActionCmd;
import globals.utils.BaseSubsystem;

public class AutonomousSubsystem extends BaseSubsystem {
  private final DrivetrainSubsystem m_drivetrainSubsystem;
  private final IntakeSubsystem m_intakeSubsystem;
  private final SendableChooser<AutoActionCmd> m_autoChooser = new SendableChooser<AutoActionCmd>();

  public AutonomousSubsystem(DrivetrainSubsystem driveSubsystem, IntakeSubsystem intakeSubsystem) {
    m_drivetrainSubsystem = driveSubsystem;
    m_intakeSubsystem = intakeSubsystem;

    loadAutoActionCmds();
    SmartDashboard.putData("Auto Selector", m_autoChooser);
  }

  public void loadAutoActionCmds() {

    m_autoChooser.addOption("1m Forward", new AutoActionCmd(this, m_drivetrainSubsystem, m_intakeSubsystem)
        .executeDrivePath("1m Forward") // went 87 cm
        .complete());
    m_autoChooser.setDefaultOption("1 Ball Auto", new AutoActionCmd(this, m_drivetrainSubsystem, m_intakeSubsystem)
        .executeIntakeFire()
        .executeAction((drivetrain, intake) -> drivetrain.drive(-Auto.DRIVE_POWER, 0, 0, false))
        .executePause(1.4)
        .complete());
    m_autoChooser.addOption("4BallAutoLeft", new AutoActionCmd(this, m_drivetrainSubsystem, m_intakeSubsystem)
        .executeArmPosition(false)
        .executeDrivePath("4BallAutoFirstStage")
        .executeArmPosition(true)
        .executeDrivePath("4BallAutoSecondStage")
        .executeIntakeFire()
        .executePause(1.69)
        .executeArmPosition(false)
        .executeDrivePath("4BallAutoThirdStage")
        .executeDrivePath("4BallAutoFourthStage")
        .executeArmPosition(true)
        .executeDrivePath("4BallAutoFithStage")
        .executeIntakeFire()
        .executePause(1.69)
        .executeDrivePath("4BallAutoSixthStage")
        .complete());

    m_autoChooser.addOption("4BallAutoRight", new AutoActionCmd(this, m_drivetrainSubsystem, m_intakeSubsystem)
        .executeArmPosition(false)
        .executeDrivePath("4BallAutoFirstStageRight")
        .executeArmPosition(true)
        .executeDrivePath("4BallAutoSecondStageRight")
        .executeIntakeFire()
        .executePause(1.69)
        .executeArmPosition(false)
        .executeDrivePath("4BallAutoThirdStage")
        .executeArmPosition(true)
        .executeDrivePath("4BallAutoFourthStage")
        .executeIntakeFire()
        .executePause(1.69)
        .executeDrivePath("4BallAutoFithStage")
        .complete());

    m_autoChooser.addOption("HangarDump", new AutoActionCmd(this, m_drivetrainSubsystem, m_intakeSubsystem)
        .executeArmPosition(false)
        .executeDrivePath("HangarDump1")
        .executeArmPosition(true)
        .executeDrivePath("HangarDump2")
        .executeIntakeFire()
        .executePause(1.69)
        .executeArmPosition(false)
        .executeDrivePath("HangarDump3")
        .executeArmPosition(true)
        .executePause(1.69)
        .executeIntakeFire()
        .executePause(1)
        .executeDrivePath("HangarDump4")
        .complete());

    m_autoChooser.addOption("2BallLeft", new AutoActionCmd(this, m_drivetrainSubsystem, m_intakeSubsystem)
        .executeArmPosition(false)
        .executeDrivePath("2BallLeft1")
        .executeArmPosition(true)
        .executeDrivePath("2BallLeft2")
        .executeIntakeFire()
        .executePause(1.69)
        .executeDrivePath("2BallLeft3")
        .complete());
  }

  public Optional<AutoActionCmd> getAutoCmd() {
    return Optional.ofNullable(m_autoChooser.getSelected());
  }

  public void showCurrentTrajectory(Trajectory trajectory) {
    RobotContainer.telemetry.setTrajectory(trajectory);
  }
}