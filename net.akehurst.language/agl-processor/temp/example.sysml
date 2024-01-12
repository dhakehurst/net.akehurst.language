package test;

part def 'System context' {
    #system   part sut:'System to Build';
    #external part 'User';
    #external part 'Other System 1';
    #external part 'Other System 2';
    #external part 'Environment';

    connect sut to 'User';
    connect sut to 'Other System 1';
    connect sut to 'Other System 2';
    connect sut to 'Environment';
}



#system part def 'Weather Sensor Unit';

requirement <'1'> {
    subject sut:'Weather Sensor Unit';
    doc /* The Unit shall detect rain level, external temperature and light levels. */
}

requirement <'2'> {
    subject sut:'Weather Sensor Unit';
    doc /* The Unit shall withstand shock and vibration typical to a standard road going vehicle */
}

requirement <'3'> {
    subject sut:'Weather Sensor Unit';
    doc /* The Unit shall weigh a maximum of 300g */

    require constraint { sut.mass <= 300[g]  }
}


part def 'System context' {
    #system   part sut:'Weather Sensor Unit';
    #external part 'Windscreen';
    #external part 'Other Vehicle ECU';
    #external part 'Diagnostics Tool';
    #external part 'Environment';

    connect sut to 'Windscreen';
    connect sut to 'Other Vehicle ECU';
    connect sut to 'Diagnostics Tool';
    connect sut to 'Environment';
}



#system part def 'Weather Sensor Unit' {
    doc purpose /* to detect the external temperature, rain & light of a vehicle */
}

part def 'System context' {
    #system   part sut:'Weather Sensor Unit';
    #external part 'Windscreen'         { doc purpose /* to provide a mounting point for the system */ }
    #external part 'Other Vehicle ECU'  { doc purpose /* to receive sensor data from the system */ }
    #external part 'Diagnostics Tool'   { doc purpose /* to view diagnostic data coming from the system */ }
    #external part 'Environment'        { doc purpose /* to provide temperature, rain & light to the system */ }

    connect sut to 'Windscreen';
    connect sut to 'Other Vehicle ECU';
    connect sut to 'Diagnostics Tool';
    connect sut to 'Environment';
}



part def 'System context' {
    #system   part sut:'Weather Sensor Unit';
    #external part 'Windscreen'         { doc purpose /* to provide a mounting point for the system */ }
    #external part 'Other Vehicle ECU'  { doc purpose /* to receive sensor data from the system */ }
    #external part 'Diagnostics Tool'   { doc purpose /* to view diagnostic data coming from the system */ }
    #external part 'Environment'        { doc purpose /* to provide temperature, rain & light to the system */ }

    connect sut to 'Windscreen';
    connect sut to 'Other Vehicle ECU';
    connect sut to 'Diagnostics Tool';
    connect sut to 'Environment';

    satisfy requirement <'1'> by 'Environment';
    satisfy requirement <'5'> by 'Diagnostics Tool';
    satisfy requirement <'6'> by 'Other Vehicle ECU';
    satisfy requirement <'8'> by 'Windscreen';
}


#system part def 'System to Build' {
    port user;
    port external1;
    port external2;
    port environment;
}


port def AnInterface {
    in attribute aReception;
    in perform action aFunction {
        in arg1;
    }
}

port def FuelPort {
    attribute temperature : Temp;
    out item fuelSupply : Fuel;
    in item fuelReturn : Fuel;
}

port def DrivePwrPort{
    out torque:Torque;
}

port def LugNutPort{
    attribute threadDia;
    attribute threadPitch;
}


use case def 'Provide Transportation' {
    subject vehicle : Vehicle;
    actor driver : Person;
    actor passengers : Person[0..4];
    actor environment : Environment;
    objective {
    doc /* Transport driver and passengers from starting
         * location to ending location.
         */
    }
}

use case def 'Provide Transportation' {
    // ...
    message of Enter from driver to vehicle;
    then message of Enter from passengers to vehicle;
    then message of Drive from vehicle to environment;
    then message of Exit from passengers to vehicle;
    then message of Exit from driver to vehicle;
}


