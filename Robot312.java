// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

//Imports that allow the usage of REV Spark Max motor controllers
import com.revrobotics.CANSparkBase;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkLowLevel.MotorType;

//Imports for LEDs, see colors in comments at end
//import edu.wpi.first.wpilibj.PWM;
//import edu.wpi.first.wpilibj.motorcontrol.Spark;
//import edu.wpi.first.wpilibj.DriverStation;

//iMPORT FOR CAMERA
import edu.wpi.first.cameraserver.CameraServer;

import com.kauailabs.navx.AHRSProtocol.AHRS_TUNING_VAR_ID;
//Import for Gyro
import com.kauailabs.navx.frc.AHRS;

//Imports for Everything Else
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.XboxController.Button;
import edu.wpi.first.wpilibj.Joystick;
//import edu.wpi.first.math.geometry.Translation2d;
//import edu.wpi.first.math.util.Units;



//Start of Main Code----------------------------------------------------------------------------------


/*Autonomous Selection Options */
public class Robot extends TimedRobot {
  private static final String kNothingAuto = "do nothing";
  private static final String kLaunchAndDrive = "launch drive";
  private static final String kLaunch = "launch";
  private static final String kDrive = "drive";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();
  AHRS ahrs;

  /*Drive motor controller instances. We named each controller based on their Device ID for the CAN */
  CANSparkBase leftRear = new CANSparkMax(2, MotorType.kBrushed);
  CANSparkBase leftFront = new CANSparkMax(4, MotorType.kBrushed);  
  CANSparkBase rightRear = new CANSparkMax(1, MotorType.kBrushed);
  CANSparkBase rightFront = new CANSparkMax(3, MotorType.kBrushed);
 
  /*
   * A class provided to control your drivetrain. Different drive styles can be passed to differential drive:
  */

  DifferentialDrive m_drivetrain;

  /*
   * Below are the motors assigned to the launchwheel and feedwheel, they should stay commented until Mechanical
   * has added the rest of the motors and capability
     */
  CANSparkBase m_launchWheel = new CANSparkMax(6, MotorType.kBrushed);
  CANSparkBase m_feedWheel = new CANSparkMax(5, MotorType.kBrushed);


  XboxController m_driverController = new XboxController(0);
  

  /*
   *If We need another controller and driver (ie, one to shoot and one to drive). Leave commented out 
  otherwise!
  /*

  //Joystick m_manipController = new Joystick(1);


  // --------------- Magic numbers. Use these to adjust settings. ---------------

 /**
   * How many amps can an individual drivetrain motor use.
   */
  static final int DRIVE_CURRENT_LIMIT_A = 60;

  /**
   * How many amps the feeder motor can use.
   */
  static final int FEEDER_CURRENT_LIMIT_A = 60;

  /**
   * Percent output to run the feeder when expelling note
   */
  static final double FEEDER_OUT_SPEED = 1.0;

  /**
   * Percent output to run the feeder when intaking note
   */
  static final double FEEDER_IN_SPEED = -.4;
  /**
   * Percent output for amp or drop note, configure based on polycarb bend
   */
  static final double FEEDER_AMP_SPEED = .4;

  /**
   * How many amps the launcher motor can use.
   *
   * In our testing we favored the CIM over NEO, if using a NEO lower this to 60
   */
  static final int LAUNCHER_CURRENT_LIMIT_A = 60;

  /**
   * Percent output to run the launcher when intaking AND expelling note
   */
  static final double LAUNCHER_SPEED = 1.0;

  /**
   * Percent output for scoring in amp or dropping note, configure based on polycarb bend
   * .14 works well with no bend from our testing
   */
  static final double LAUNCHER_AMP_SPEED = .14;

  /**
   * When the robot starts, the code below begins to run (INITIALIZATION)
   */

  @Override
  public void robotInit() {
    CameraServer.startAutomaticCapture();
    m_chooser.setDefaultOption("do nothing", kNothingAuto);
    m_chooser.addOption("launch note and drive", kLaunchAndDrive);
    m_chooser.addOption("launch", kLaunch);
    m_chooser.addOption("drive", kDrive);
    SmartDashboard.putData("Auto choices", m_chooser);

    //Getting started color with LED
    //Spark spark = new Spark(0); // 0 is the RIO PWM port this is connected to
    //spark.set(-0.75); // the % output of the motor, between -1 and 1
    

    /*
     * Apply the current limit to the drivetrain motors
     */
    leftRear.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    leftFront.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    rightRear.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    rightFront.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);

    /*
     * Tells the rear wheels to follow the same commands as the front wheels
     */
    leftRear.follow(leftFront);
    rightRear.follow(rightFront);

    /*
     * One side of the drivetrain must be inverted, as the motors are facing opposite directions
     */
    leftFront.setInverted(true);
    rightFront.setInverted(false);

    m_drivetrain = new DifferentialDrive(leftFront, rightFront);

    m_feedWheel.setInverted(true);
    m_launchWheel.setInverted(true);
/*
* Apply the current limit to the launching mechanism
*/
    m_feedWheel.setSmartCurrentLimit(FEEDER_CURRENT_LIMIT_A);
    m_launchWheel.setSmartCurrentLimit(LAUNCHER_CURRENT_LIMIT_A);
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test modes.
   */
  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Time (seconds)", Timer.getFPGATimestamp());
  }

//--------------------------------------------AUTONOMOUS----------------------------------------//

  /*
   * Auto constants, change values below in autonomousInit()for different autonomous behaviour
   *
   * A delayed action starts X seconds into the autonomous period
   *
   * A time action will perform an action for X amount of seconds
   *
   * Speeds can be changed as desired and will be set to 0 when
   * performing an auto that does not require the system
   */
  
  double AUTO_LAUNCH_DELAY_S;
  double AUTO_DRIVE_DELAY_S;
  double AUTO_DRIVE_TIME_S;
  double AUTO_DRIVE_SPEED;
  double AUTO_LAUNCHER_SPEED;
  double autonomousStartTime;

  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();

    leftRear.setIdleMode(IdleMode.kBrake);
    leftFront.setIdleMode(IdleMode.kBrake);
    rightRear.setIdleMode(IdleMode.kBrake);
    rightFront.setIdleMode(IdleMode.kBrake);

    AUTO_LAUNCH_DELAY_S = 2;
    AUTO_DRIVE_DELAY_S = 3;
    AUTO_DRIVE_TIME_S = 2.0;
    AUTO_DRIVE_SPEED = -0.5;
    AUTO_LAUNCHER_SPEED = 1;
    
    /*
     * Depending on which auton is selected, speeds for the unwanted subsystems are set to 0
     * if they are not used for the selected auton
     *
     * For kDrive you can also change the kAutoDriveBackDelay
     */
    if(m_autoSelected == kLaunch)
    {
      AUTO_DRIVE_SPEED = 0;
      AUTO_LAUNCHER_SPEED = 1;
      //Change LED to indicate kLaunch
   
    }
    else if(m_autoSelected == kDrive)
    {
      AUTO_LAUNCHER_SPEED = 0;
      AUTO_DRIVE_SPEED = -0.5;
    }
    else if(m_autoSelected == kLaunchAndDrive)
    {
      AUTO_DRIVE_SPEED = -0.5;
      AUTO_LAUNCHER_SPEED = 1;
    }
    else if(m_autoSelected == kNothingAuto)
    {
      AUTO_DRIVE_SPEED = 0;
      AUTO_LAUNCHER_SPEED = 0;
    }

    autonomousStartTime = Timer.getFPGATimestamp();
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {

    double timeElapsed = Timer.getFPGATimestamp() - autonomousStartTime;

    /*
     * Spins up launcher wheel until time spent in auto is greater than AUTO_LAUNCH_DELAY_S
     *
     * Feeds note to launcher until time is greater than AUTO_DRIVE_DELAY_S
     *
     * Drives until time is greater than AUTO_DRIVE_DELAY_S + AUTO_DRIVE_TIME_S
     *
     * Does not move when time is greater than AUTO_DRIVE_DELAY_S + AUTO_DRIVE_TIME_S
     */
    if(timeElapsed < AUTO_LAUNCH_DELAY_S)
    {
      m_launchWheel.set(AUTO_LAUNCHER_SPEED);
      m_drivetrain.arcadeDrive(0, 0);

    }
    else if(timeElapsed < AUTO_DRIVE_DELAY_S)
    {
      m_feedWheel.set(AUTO_LAUNCHER_SPEED);
      m_drivetrain.arcadeDrive(0, 0);
    }
    else if(timeElapsed < AUTO_DRIVE_DELAY_S + AUTO_DRIVE_TIME_S)
    {
      m_launchWheel.set(0);
      m_feedWheel.set(0);
      m_drivetrain.arcadeDrive(AUTO_DRIVE_SPEED, 0);
    }
    else
    {
      m_drivetrain.arcadeDrive(0, 0);
    }
    /* For an explanation on differintial drive, squaredInputs, arcade drive and tank drive see the bottom of this file */
  }



//----------------------------TELEOP---------------------------------------------------------//

  /** This function is called once when teleop is enabled. */
  @Override
  public void teleopInit() {
    /*
     * Motors can be set to idle in brake or coast mode.
     *
     * Brake mode effectively shorts the leads of the motor when not running, making it more
     * difficult to turn when not running.
     *
     * Coast doesn't apply any brake and allows the motor to spin down naturally with the robot's momentum.
     *
     * (touch the leads of a motor together and then spin the shaft with your fingers to feel the difference)
     *
     * This setting is driver preference. Try setting the idle modes below to kBrake to see the difference.
     */
    leftRear.setIdleMode(IdleMode.kBrake);
    leftFront.setIdleMode(IdleMode.kBrake);
    rightRear.setIdleMode(IdleMode.kBrake);
    rightFront.setIdleMode(IdleMode.kBrake);
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {

    /*
     * Spins up the launcher wheel
     */
    if (m_driverController.getRawButton(1)) {
      m_launchWheel.set(LAUNCHER_SPEED);
    }
    else if(m_driverController.getRawButtonReleased(1))
    {
      m_launchWheel.set(0);
    }
    /*
     * Spins feeder wheel, wait for launch wheel to spin up to full speed for best results
     */
    if (m_driverController.getRawButton(6))
    {
     m_feedWheel.set(FEEDER_OUT_SPEED);
    }
    else if(m_driverController.getRawButtonReleased(6))
    {
     m_feedWheel.set(0);
    }

    /*
     * While the button is being held spin both motors to intake note
     */
    if (m_driverController.getRawButton(5))
    {
     m_launchWheel.set(-LAUNCHER_SPEED);
     m_feedWheel.set(FEEDER_IN_SPEED);
    }
    else if(m_driverController.getRawButtonReleased(5))
    {
     m_launchWheel.set(0);
     m_feedWheel.set(0);
    }

    /*
     * While the amp button is being held, spin both motors to "spit" the note
     * out at a lower speed into the amp
     *
     * (this may take some driver practice to get working reliably)
     */
    if(m_driverController.getRawButton(2))
    {
      m_feedWheel.set(FEEDER_AMP_SPEED);
      m_launchWheel.set(LAUNCHER_AMP_SPEED);
    }
    else if(m_driverController.getRawButtonReleased(2))
    {
      m_feedWheel.set(0);
      m_launchWheel.set(0);
    }

    /*
     * Negative signs are here because the values from the analog sticks are backwards
     * from what we want. Pushing the stick forward returns a negative when we want a
     * positive value sent to the wheels.
     *
     * If you want to change the joystick axis used, open the driver station, go to the
     * USB tab, and push the sticks determine their axis numbers
     */
    m_drivetrain.arcadeDrive(-m_driverController.getLeftY(), -m_driverController.getRightX(), false);
  }
  //------------------------------------------COLORS-------------------------------------------//
  
  //Colors for LEDs UNTESTED
  public static class BlinkinConstants {
    public static final double BLINKIN_RED = 0.61;
    public static final double BLINKIN_BLUE = 0.87;
    public static final double BLINKIN_YELLOW = 0.69;
    public static final double BLINKIN_VIOLET = 0.91;
    public static final double BLINKIN_RAINBOW = -0.99;
    public static final double BLINKIN_HOT_PINK = 0.57;
    public static final double BLINKIN_GREEN = 0.77;
    public static final double BLINKIN_FADE_TO_BLACK = -0.03;
    public static final double BLINKIN_RAINBOW_WAVE = -0.45;
    public static final double BLINKIN_RAINBOW_SINELON = -0.45;
    public static final double BLINKIN_CONFETTI = -0.87;
    public static final double BLINKIN_FIRE = -0.59;
    public static final double BLINKIN_GLITTER = -0.89;
    public static final double BLINKIN_PARTY_WAVE = -0.43;
    public static final double BLINKIN_SHOT_RED = -0.85;
  }
}
