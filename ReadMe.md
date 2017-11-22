# Boarding Pass Refactoring Problem

DD Airways allows passengers to checkin using following channels:

* Checkin at the airport counter.
* Checkin at airport kiosk.
* Online Checkin using Desktop computer.
* Online Checkin using Mobile phone/tablet.


Based on the channel used by the passenger to checkin, following types of boarding passes are generated:

* Checkin at the airport counter or kiosk produces a PDf boarding pass  for printing.
* Online checkin using Mobile Phone/tablet produces a Calendar entry (.ics file) with Mobile boarding pass (.pdf file) as an attachment.
* Online checkin using Desktop computer produces a zipped file (.zip) containing the following
    * Electronic boarding pass (.psf file)
    * Calendar entry (.ics file) with Mobile boarding pass (.pdf file) as an attachment.

