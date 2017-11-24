# Boarding Pass Refactoring Problem

DD Airways allows passengers to checkin using following channels:

* Checkin at the airport counter.
* Checkin at airport kiosk.
* Online Checkin using Desktop computer.
* Online Checkin using Mobile phone/tablet.


Based on the channel used by the passenger to checkin, following types of boarding passes are generated:

* Airport Checkin at the counter or kiosk produces a PDf boarding pass for printing.
* Online checkin using Desktop computer produces a zipped file (```.zip```) containing the following
    * Electronic boarding pass (```.pdf``` file)
    * Calendar entry (```.ics``` file) with Mobile boarding pass (```.pdf``` file) as an attachment.
* Online checkin using Mobile Phone/Tablet produces two files:
    * A Mobile boarding pass (```.pdf``` file) 
    * An Calendar entry (```.ics``` file) with Mobile boarding pass (```.pdf``` file) as an attachment to it.
    * NOTE: For mobile checkin, a zip is not produced, because one may not have an unzip utility on their devices.

## Build
* To build, simply run ```gradle```
* To generate Eclipse project: use ```gradle cleanEclipse eclipse```
* To generate an Idea project: use ```gradle cleanIdea idea```