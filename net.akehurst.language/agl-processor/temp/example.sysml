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

//1

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

//2
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

//3
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

//4
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

//5
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

//6
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

//7
use case def <'UC-1.1'> 'Use Case 1' {
  subject stb: SystemToBuild;
  actor user: User;
  actor other1: OtherSystem2;
  actor other2: OtherSystem2;
  actor env: Environment;

  #scenario occurrence def <'SC-1.1'> 'Scenario 1' {
    message of 'switch on' from user to stb;
    then message of 'send signal' from stb to other2;
    then message of 'sense flow' from env to stb;
    then message of 'send signal' from stb to other2;
    then message of 'start flow' from stb to other1;
  }
}

//8
use case def <'UC-1.1'> 'Use Case 1' {
  subject stb: SystemToBuild;
  actor user: User;
  actor other1: OtherSystem2;
  actor other2: OtherSystem2;
  actor env: Environment;

  #scenario occurrence def <'SC-1.1'> 'Scenario 1' {
    message of 'switch on' from user to stb;
    then message of 'send signal' from stb to other2;
    then flow t of :Temperature from env to stb;
    then message of 'send signal' from stb to other2;
    then flow m of :Pulse from stb to other1;
  }
}

//9
use case def <'UC-1.1'> 'Use Case 1' {
  subject stb: SystemToBuild;
  actor user: User;
  actor other1: OtherSystem2;
  actor other2: OtherSystem2;
  actor env: Environment;

  #scenario action def <'SC-1.1'> 'Scenario 1' {
    message of 'switch on' from user to stb;
    then message of 'send signal' from stb to other2;
    then flow t of :Temperature from env to stb;
    then decide;
      if 'flow detected' then yes;

    action yes;
      message of 'send signal' from stb to other2;
      then flow m of :Pulse from stb to other1;
  }

}

//10
use case def <'UC-1.1'> 'Use Case 1' {
  subject stb: SystemToBuild;
  actor user: User;
  actor other1: OtherSystem2;
  actor other2: OtherSystem2;
  actor env: Environment;

  #scenario action def <'SC-1.2'> 'Scenario 2' {
    message of 'switch on' from user to stb;
    then message of 'send signal' from stb to other2;
    then flow t of :Temperature from env to stb;
    then decide;
      if 'flow NOT detected' then no;

    action no;
        message of 'send error' from stb to other2;
  }

}

//11
use case def <'UC-1.1'> 'Use Case 1' {
  subject stb: SystemToBuild;
  actor user: User;
  actor other1: OtherSystem2;
  actor other2: OtherSystem2;
  actor env: Environment;

  #expected action def 'Use Case Behaviour' {
    message of 'switch on' from user to stb;
    then message of 'send signal' from stb to other2;
    then flow t of :Temperature from env to stb;
    then decide;
      if 'flow detected' then yes;
      if 'flow NOT detected' then no;

    action yes;
      message of 'send signal' from stb to other2;
      then flow m of :Pulse from stb to other1;

    action no;
        message of 'send error' from stb to other2;
  }

}

//12
use case def <'UC-1.1'> 'Use Case 1' {
  subject stb: SystemToBuild;
  actor user: User;
  actor other1: OtherSystem2;
  actor other2: OtherSystem2;
  actor env: Environment;

  #expected action def 'Use Case Behaviour' {
    accept 'switch on' via user;
    then send 'signal' to other2;
    then flow t of :Temperature from env to stb;
    then decide;
      if 'flow detected' then yes;
      if 'flow NOT detected' then no;

    action yes;
      then send 'signal' to other2;
      then flow m of :Pulse from stb to other1;

    action no;
      send 'error' to other2;
  }

}

//13
use case <'UC-1'> 'Startup' {
    doc purpose /* To highlight the startup behaviour of the system */

    subject stb:'Weather Sensor Unit';
    actor pwr: 'Power/Ignition';
}

use case <'UC-2'> 'Sense Weather' {
    doc purpose /* To detect the temperature, rain and light levels
                   and provide this data to ‘OtherVehECU’ */

    subject stb:'Weather Sensor Unit';
    actor oth: 'Other Vehicle ECU';
    actor env: 'Environment';
}

//14
use case <'UC-3'> '???' {
    doc purpose /*  */

    subject stb:'Weather Sensor Unit';
    actor pwr: 'Power/Ignition';
    actor wsc: 'Windscreen';


}