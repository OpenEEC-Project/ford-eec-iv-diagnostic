package babroval.eec_iv.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import babroval.eec_iv.util.FileUtils;
import babroval.eec_iv.view.StartView;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class StartController extends Thread {

	private static final String FILE_FAULTS_PATH = "EECIVFaults.txt";

	private static SerialPort serialPort = new SerialPort("COM7");;

	private static StartView view = new StartView();

	private static List<String> allFaults = new ArrayList<String>();
	private static List<String> faults = new ArrayList<String>();
	private static Boolean timeOut = new Boolean(true);

	{
		allFaults = FileUtils.loadAllFaults(FILE_FAULTS_PATH);
	}

	public StartController() {
	}

	public void initController() {

		resetFrame();
		view.getDisconnect().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {

					serialPort.closePort();
					resetFrame();

				} catch (SerialPortException ex) {
					JOptionPane.showMessageDialog(view.getPanel(), "port closing failed", "",
							JOptionPane.ERROR_MESSAGE);
					view.dispose();
				}

			}
		});

		view.getFaults().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {

				try {

					serialPort.openPort();

					serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_2,
							SerialPort.PARITY_NONE);

					serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);

					Thread.sleep(1000);

					view.getFaults().setEnabled(false);
					view.getBaud().setEnabled(false);
					view.getDisconnect().setEnabled(true);
					view.getLabelConnect().setText("Interface is connected");

					if (view.getBaud().isSelected()) {
						serialPort.writeByte((byte) 4);
					} else {
						serialPort.writeByte((byte) 5);
					}

					Thread.sleep(500);

					serialPort.writeByte((byte) 1);

					Thread.sleep(12000);

					if (timeOut.booleanValue()) {
						JOptionPane.showMessageDialog(view.getPanel(),
								"Switch the ignition OFF, reconnect diagnostic cable in the car, select checkbox '19200', switch the ignition ON and press 'FAULTS' button again",
								"", JOptionPane.WARNING_MESSAGE);
						serialPort.closePort();
						resetFrame();
					}

				} catch (Exception e) {
					JOptionPane.showMessageDialog(view.getPanel(), "no interface connection, check USB wiring", "",
							JOptionPane.ERROR_MESSAGE);
					resetFrame();

				}
			}
		});
	}

	private static class PortReader implements SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR() && event.getEventValue() > 0) {
				try {
					timeOut = Boolean.FALSE;
					String receivedData = serialPort.readHexString(event.getEventValue());
					System.out.println(receivedData);
					if (receivedData.equals("19 A1")) {
						JOptionPane.showMessageDialog(view.getPanel(),
								"There is no connection with the engine control unit (switch the ignition ON)", "",
								JOptionPane.ERROR_MESSAGE);
						serialPort.closePort();
						resetFrame();
					} else {
						Integer i = Integer.valueOf(receivedData.substring(4, 5) + receivedData.substring(0, 2));
						faults.add(allFaults.get(i - 1));

						String s = "<html>";
						for (String fault : faults) {
							s = s + fault + "<br>";
						}
						view.getLabel().setText(s);
					}
				} catch (SerialPortException ex) {
					System.out.println("Error in receiving string from COM-port: " + ex);
				}
			}
		}
	}

	private static void resetFrame() {
		faults.clear();
		timeOut = Boolean.TRUE;
		view.getFaults().setEnabled(true);
		view.getBaud().setEnabled(true);
		view.getKoeo().setEnabled(false);
		view.getData().setEnabled(false);
		view.getDisconnect().setEnabled(false);
		view.getLabel().setText("");
		view.getLabelConnect().setText(
				"The interface is disconnected. Connect the diagnostic cable in the car, switch the Ignition OFF then ON and press 'FAULTS' button");
	}
}
