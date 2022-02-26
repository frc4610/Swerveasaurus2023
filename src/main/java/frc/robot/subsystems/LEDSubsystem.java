package frc.robot.subsystems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ctre.phoenix.ErrorCode;
import com.ctre.phoenix.led.*;
import com.ctre.phoenix.led.CANdle.LEDStripType;

import edu.wpi.first.math.Pair;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.*;

class LED {
  public boolean isEnabled = false;
  private int m_r;
  private int m_g;
  private int m_b;

  LED() {
    setColor(0, 0, 0);
    isEnabled = false;
  }

  LED(int r, int g, int b) {
    setColor(r, g, b);
    isEnabled = false;
  }

  LED(int r, int g, int b, boolean enabled) {
    setColor(r, g, b);
    isEnabled = enabled;
  }

  public int getRed() {
    return m_r;
  }

  public int getGreen() {
    return m_g;
  }

  public int getBlue() {
    return m_b;
  }

  public void setColor(int r, int g, int b) {
    m_r = r;
    m_g = g;
    m_b = b;
  }

  // returns bitwise of color //0xFFFFFF
  public int getColorMask() {
    return (m_r << 16) + (m_g << 8) + m_b;
  }
}

public class LEDSubsystem extends SubsystemBase {
  private final CANdle m_ledController;
  private final CANdleConfiguration m_ledControllerConfig;
  private final CANdleFaults m_faults;

  public static int LED_STRIP_COUNT = 60;
  public static int LED_COUNT = 68;

  private static List<Pair<Integer, LED>> m_ledMap = new ArrayList<>();

  public LEDSubsystem() {
    m_ledController = new CANdle(Ids.LED_CANDLE);
    m_ledControllerConfig = new CANdleConfiguration();
    m_faults = new CANdleFaults();

    m_ledControllerConfig.stripType = LEDStripType.GRB; // led strip type
    m_ledControllerConfig.brightnessScalar = 0.5; // dim the LEDs to half brightness
    m_ledControllerConfig.disableWhenLOS = true; // when losing connection turn off
    m_ledController.configAllSettings(m_ledControllerConfig);

    for (int i = 0; i < LED_COUNT; i++) {
      m_ledMap.add(Pair.of(i, new LED(0, 0, 0)));
    }
  }

  public ErrorCode setAnimation(Animation anim) {
    return m_ledController.animate(anim);
  }

  public ErrorCode getLastError() {
    return m_ledController.getLastError();
  }

  public ErrorCode getFaults() {
    return m_ledController.getFaults(m_faults);
  }

  public void setStatusLEDColor(int r, int g, int b, int idx) {
    setLEDColor(r, g, b, idx);
  }

  public void setLEDStripColor(int r, int g, int b) {
    for (int idx = 8; idx < 68; idx++) {
      setLEDColor(r, g, b, idx);
    }
  }

  public void setLEDColor(int r, int g, int b, int idx) {
    m_ledMap.set(idx, Pair.of(idx, new LED(r, g, b, true)));
  }

  @Override
  public void periodic() {
    // FIXME: buffer this so we don't send it all at once overloading the CAN bus
    // FIXME: use a sorting alg to group together led colors reducing buffer size
    // We currently send 1632 bytes of updates per periodic

    Map<Integer, List<Pair<Integer, LED>>> groupedColorMap = new HashMap<Integer, List<Pair<Integer, LED>>>();

    // Group colors
    for (var entry : m_ledMap) {
      int colorHash = entry.getSecond().getColorMask();
      List<Pair<Integer, LED>> group = groupedColorMap.get(colorHash);
      if (group == null) {
        group = new ArrayList<Pair<Integer, LED>>();
        groupedColorMap.put(colorHash, group);
      }
      group.add(entry);
    }
    for (var group : groupedColorMap.entrySet()) {
      Collections.sort(group.getValue(), new Comparator<Pair<Integer, LED>>() {
        @Override
        public int compare(Pair<Integer, LED> lhs, Pair<Integer, LED> rhs) {
          // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
          return lhs.getFirst() > rhs.getFirst() ? -1 : (lhs.getFirst() < rhs.getFirst()) ? 1 : 0;
        }
      });
      // check where idx changes in sort

      // Update Leds if Led Enabled then Set enabled false
    }
    // This method will be called once per scheduler run
    if (getLastError() != ErrorCode.OK) {
      DriverStation.reportWarning("LEDSubsystem " + getLastError().name(), false);
    }
    if (m_faults.hasAnyFault()) {
      DriverStation.reportWarning("LEDSubsystem fault bitwise " + Long.toString(m_faults.toBitfield()), false);
    }
  }
}