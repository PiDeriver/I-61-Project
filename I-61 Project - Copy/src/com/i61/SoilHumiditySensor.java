package com.i61;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

import java.io.IOException;
/**
 * created to read a MCP3008 AtoD Chip
 *
 * @author Robert Savage
 * @author Kevin Sikes
 * @version 6/27/2018
 */
public class SoilHumiditySensor extends AdcSensor {
    private String name = "ADC";
    public float upperBound;//Sensor's physical capibilities
    public float lowerBound;
    public int[] types;
    public String[] typesName;
    public int pin;
    public int adcChannel;

    // SPI device
    public static SpiDevice spi = null;

    // ADC channel count
    //public static short ADC_CHANNEL_COUNT = 8;  // MCP3004=4, MCP3008=8



    /**
     * Constructor for objects of class Sensor
     */
    public SoilHumiditySensor(float upperBound, float lowerBound, int[] types, String[]typesName, int pin, int adcChannel)
    {
        super(upperBound,lowerBound,types,typesName, pin);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.types = types;
        this.typesName = typesName;
        this.pin = pin;
        this.adcChannel = adcChannel;
    }


    /**
     * x = 1000 to 500
     * x - 500 = 500 to 0
     * (500 - (x - 500)) * 100 / 500 (when x=500 return 100, when x=1000 return 0)
     * @param type (AtoD Converter channel)
     * @return %soil humidity
     */
    public float readData(int type){
        try{
            float humidity = readDataWithExceptions(adcChannel);
            if(humidity <=550){
                return 100;
            }
            else if(humidity >= 950){
                return 0;
            }
            else {
                return (((500 - (humidity - 500))*100) / 500);
            }
        }
        catch (IOException e){
            System.out.println("AtoD Chip IO exception");
            e.printStackTrace();
            return -1f;
        }
    }
    public float readDataWithExceptions(int type)throws IOException{
        spi = SpiFactory.getInstance(SpiChannel.CS0,
                SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
                SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0
        short channel = Short.parseShort(Integer.toString(adcChannel));
        int conversion_value = getConversionValue(channel);
        return conversion_value;
    }

    public String getName(){
        return name;
    }

    /**
     * Communicate to the ADC chip via SPI to get single-ended conversion value for a specified channel.
     * @param channel analog input channel on ADC chip
     * @return conversion value for specified analog input channel
     * @throws IOException
     */
    public static int getConversionValue(short channel) throws IOException {

        // create a data buffer and initialize a conversion request payload
        byte data[] = new byte[] {
                (byte) 0b00000001,                              // first byte, start bit
                (byte)(0b10000000 |( ((channel & 7) << 4))),    // second byte transmitted -> (SGL/DIF = 1, D2=D1=D0=0)
                (byte) 0b00000000                               // third byte transmitted....don't care
        };

        // send conversion request to ADC chip via SPI channel
        byte[] result = spi.write(data);

        // calculate and return conversion value from result bytes
        int value = (result[1]<< 8) & 0b1100000000; //merge data[1] & data[2] to get 10-bit result
        value |=  (result[2] & 0xff);
        return value;
    }

}
