# MercuryPowerMeter Binding

This binding supports Mercury M20x line of smart power meters, produced by Russian company named Incotex Electronics Group (https://www.incotexcom.ru/catalogue)

## Supported Things

- Serial bus (Mercury-221 or any other serial line adapter)
- M200 counter - Mercury-200 single-phase AC meter. All single-phase meters, made by this company, should be compatible.

Note that three-phase counters from the same vendor have different, incompatible protocol, and therefore are not
currently supported by this binding

## Discovery

Due to nature of serial bus being used, no automatic discovery is possible.

## Thing Configuration

### Mercury Serial Bus Bridge (id "serial_bus")

| Parameter | Meaning                                                 |
|-----------|---------------------------------------------------------|
| port      | Serial port name to use                                 |
| baud      | Baud rate to use for the communication. Default is 9600 |

### Mercury 20x Thing (id "mercury200")

| Parameter     | Meaning                                                 |
|---------------|---------------------------------------------------------|
| address       | Address of the meter on the serial bus. For Mercury 200 defaults to last 6 digits of the serial number; for other models please see the respective manual |
| poll_interval | Polling interval in seconds                             |

## Channels

| channel     | type   | description                                   |
|-------------|--------|-----------------------------------------------|
| energy1     | Number | Total energy accounted for Tariff #1, Kwt*H   |
| energy2     | Number | Total energy accounted for Tariff #2, Kwt*H   |
| energy3     | Number | Total energy accounted for Tariff #3, Kwt*H   |
| energy4     | Number | Total energy accounted for Tariff #4, Kwt*H   |
| battery     | Number | Voltage of built-in lithium backup battery, V |
| num_tariffs | Number | Number of active tariffs (1 - 4)              |
| tariff      | Number | Number of tariff currently being used (1 - 4) |
| voltage     | Number | AC line voltage, V                            |
| current     | Number | AC line current, A                            |
| power       | Number | AC line power (current), W                    |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._
