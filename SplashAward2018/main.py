import sys
import paho.mqtt.client as mqtt
import json
from PySide import QtGui, QtCore
from PySide.QtCore import Qt


class SplashAwardGUI(QtGui.QMainWindow):
    def __init__(self):
        super(SplashAwardGUI, self).__init__()

        self.setupDestination("Bukit Panjang Plaza", True)

        self.initUI()

    def initUI(self):
        p = self.palette()
        p.setColor(self.backgroundRole(), Qt.black)
        self.setCentralWidget(self.destinationScreen)
        self.setAutoFillBackground(True)
        self.setPalette(p)
        #self.showMaximized()
        self.show()


    def setupFirstScreen(self):
        self.firstScreen = QtGui.QWidget()
        hbox = QtGui.QHBoxLayout()
        hbox.addStretch()
        vbox = QtGui.QVBoxLayout()
        vbox.addStretch()

        bussa = QtGui.QLabel("  Welcome to BUSSA.")
        wave = QtGui.QLabel("Wave at Robot to begin")
        p = bussa.palette()
        p.setColor(bussa.foregroundRole(), Qt.lightGray)
        font = QtGui.QFont ("Calibri", 72)
        bussa.setFont(font)
        bussa.setPalette(p)
        wave.setFont(font)
        wave.setPalette(p)
        vbox.addWidget(bussa)
        vbox.addWidget(wave)
        vbox.addStretch()
        hbox.addLayout(vbox)
        hbox.addStretch()
        self.firstScreen.setLayout(hbox)



    def setupHIScreen(self):
        self.secondScreen = QtGui.QWidget()
        hbox = QtGui.QHBoxLayout()
        hbox.addStretch()
        vbox = QtGui.QVBoxLayout()
        vbox.addStretch()

        bussa = QtGui.QLabel("Hi.")
        p = bussa.palette()
        p.setColor(bussa.foregroundRole(), Qt.lightGray)
        font = QtGui.QFont("Calibri", 72)
        bussa.setFont(font)
        bussa.setPalette(p)

        vbox.addWidget(bussa)
        vbox.addStretch()
        hbox.addLayout(vbox)
        hbox.addStretch()
        self.secondScreen.setLayout(hbox)

    def setupChoiceScreen(self):
        self.thirdScreen = QtGui.QWidget()
        hbox = QtGui.QHBoxLayout()
        hbox.addStretch()
        vbox = QtGui.QVBoxLayout()


        whatyouwant = QtGui.QLabel("Speak your mind")
        reminder = QtGui.QLabel("Reminder")
        route = QtGui.QLabel("Route planning")
        bustiming = QtGui.QLabel("Bus timing")
        p = whatyouwant.palette()
        reminderPalette = reminder.palette()
        p.setColor(whatyouwant.foregroundRole(), Qt.lightGray)

        reminderPalette.setColor(whatyouwant.foregroundRole(), Qt.white)
        font = QtGui.QFont("Calibri", 72)
        fontBigger = QtGui.QFont("Calibri", 80)


        whatyouwant.setFont(font)
        whatyouwant.setPalette(p)

        reminder.setFont(fontBigger)
        reminder.setPalette(reminderPalette)

        bustiming.setFont(fontBigger)
        bustiming.setPalette(reminderPalette)

        route.setFont(fontBigger)
        route.setPalette(reminderPalette)
        vbox.addStretch(1)
        vbox.addWidget(whatyouwant)
        vbox.addWidget(reminder)
        vbox.addWidget(route)
        vbox.addWidget(bustiming)

        vbox.addStretch(2)
        hbox.addLayout(vbox)
        hbox.addStretch()
        self.thirdScreen.setLayout(hbox)

    def setupDestination(self, destination = None, wait = False):
        self.destinationScreen = QtGui.QWidget()
        hbox = QtGui.QHBoxLayout()
        hbox.addStretch()
        vbox = QtGui.QVBoxLayout()

        whatyouwant = QtGui.QLabel("Your destination please")
        reminder = None
        fontBigger = QtGui.QFont("Calibri", 80)

        if not destination == None:
            reminder = QtGui.QLabel(destination)
            reminderPalette = reminder.palette()
            reminderPalette.setColor(whatyouwant.foregroundRole(), Qt.white)
            reminder.setPalette(reminderPalette)
            reminder.setFont(fontBigger);

        p = whatyouwant.palette()
        p.setColor(whatyouwant.foregroundRole(), Qt.lightGray)
        whatyouwant.setPalette(p)
        whatyouwant.setFont(fontBigger)


        vbox.addStretch(1)
        vbox.addWidget(whatyouwant)

        if not destination == None:
            vbox.addWidget(reminder)
        if wait:
            waitLabel =QtGui.QLabel("Please wait.");
            p = waitLabel.palette()
            p.setColor(waitLabel.foregroundRole(), Qt.blue);
            waitLabel.setPalette(p)
            font = QtGui.QFont("Calibri", 20)
            waitLabel.setFont(font);
            vbox.addWidget(waitLabel)


        vbox.addStretch(2)
        hbox.addLayout(vbox)
        hbox.addStretch()
        self.destinationScreen.setLayout(hbox)


    def displayBusStop(self, busStopInfo, timingVar = False):
        if busStopInfo == None:
            busStopInfo = '''
           {"stopno":"44151","busname":"Opp Bt Batok Fire Stn","buses":[{"serviceno":"160","reminded":false,"distance":4186.433564978818,"time":9.162283333333333},{"serviceno":"180","reminded":true,"distance":4022.4184709289925,"time":9.112283333333332},{"serviceno":"187","reminded":false,"distance":416.68627459649645,"time":-0.7543833333333333},{"serviceno":"188","reminded":false,"distance":950.4956709687392,"time":1.9622833333333334},{"serviceno":"985","reminded":false,"distance":682.3001403505085,"time":0.045616666666666666}]} 
            '''

        busStopInfo = json.loads(busStopInfo)

        fontSmall = QtGui.QFont("Calibri", 30)

        font = QtGui.QFont("Calibri", 60)

        hbox = QtGui.QHBoxLayout()

        busStopLabel = QtGui.QLabel("Bus Stop No")
        busStopLabel.setFont(fontSmall)

        p = busStopLabel.palette()
        p.setColor(busStopLabel.foregroundRole(), Qt.lightGray)
        busStopLabel.setPalette(p)


        busStopLabel2 = QtGui.QLabel(busStopInfo['stopno'])
        busStopLabel2.setFont(font)
        busStopPallete = busStopLabel2.palette()
        busStopPallete.setColor(busStopLabel2.foregroundRole(), Qt.white)
        busStopLabel2.setPalette(busStopPallete)

        hbox.addWidget(busStopLabel)
        hbox.addSpacing(40)
        hbox.addWidget(busStopLabel2)


        hbox.addStretch()

        hboxLine2 = QtGui.QHBoxLayout()

        busStopName = QtGui.QLabel("Bus Stop Name")
        busStopName.setFont(fontSmall)
        busStopName.setPalette(p)

        busNameLabel = QtGui.QLabel(busStopInfo['busname'])
        busNameLabel.setFont(font)
        busNameLabel.setPalette(busStopPallete)
        hboxLine2.addWidget(busStopName)
        hbox.addSpacing(40)
        hboxLine2.addWidget(busNameLabel)
        hboxLine2.addStretch()


        vGridBoxBuses = QtGui.QGridLayout()
        vGridBoxBuses.setColumnStretch(0, 10)
        vGridBoxBuses.setColumnStretch(1, 3)
        vGridBoxBuses.setColumnStretch(2, 15)
        vGridBoxBuses.setColumnStretch(3, 15)
        vGridBoxBuses.setColumnStretch(4, 65)
        font = QtGui.QFont("Courier", 38)
        service = QtGui.QLabel("Service")
        service.setFont(fontSmall)

        tto = QtGui.QLabel("Time To Arrive")
        tto.setFont(fontSmall)

        p = tto.palette()
        p.setColor(tto.foregroundRole(), Qt.darkBlue)


        distanceLabel= QtGui.QLabel("Distance")

        tto.setPalette(p)
        service.setPalette(p)
        distanceLabel.setPalette(p)
        distanceLabel.setFont(fontSmall)

        vGridBoxBuses.addWidget(service, 0, 0);

        vGridBoxBuses.addWidget(tto, 0, 2);
        vGridBoxBuses.addWidget(distanceLabel, 0, 3);


        for index, bus in enumerate(busStopInfo['buses']):
            row = index / 1 + 1
            col = index % 1
            print row, col

            label = QtGui.QLabel(bus['serviceno'])

            p = label.palette()
            p.setColor(label.foregroundRole(), Qt.white)
            if bus['reminded']:
                label.setAutoFillBackground(True)
                p.setColor(label.backgroundRole(), Qt.green);

            label.setFont(font)
            label.setFrameShape(QtGui.QFrame.Panel)
            label.setLineWidth(1)
            label.setPalette(p)
            vGridBoxBuses.addWidget(label, row, col);

            label = QtGui.QLabel('{0:6.2f}'.format(bus['time']) + " mins")
            label.setFont(font)
            label.setFrameShape(QtGui.QFrame.Panel)
            label.setLineWidth(1)

            p = label.palette()
            p.setColor(label.foregroundRole(), Qt.white)

            label.setPalette(p)

            vGridBoxBuses.addWidget(label, row, col + 2);

            label = QtGui.QLabel('{0:6.2f}'.format(bus['distance']) + " m")
            label.setFont(font)
            label.setFrameShape(QtGui.QFrame.Panel)
            label.setLineWidth(1)
            label.setPalette(p)
            vGridBoxBuses.addWidget(label, row, col + 3);




        vMainBox = QtGui.QVBoxLayout()
        vMainBox.addLayout(hbox)
        vMainBox.addLayout(hboxLine2)
        busesLabel = QtGui.QLabel("Buses")
        busesLabel.setPalette(p)
        busesLabel.setFont(fontSmall)
        vMainBox.addWidget(busesLabel)
        vMainBox.addSpacing(10)
        vMainBox.addLayout(vGridBoxBuses)
        vMainBox.addStretch()


        reminderLabel = QtGui.QLabel("Green : reminded" )
        if timingVar:
            reminderLabel = QtGui.QLabel("Green : reminded. Wave to go main menu" )

        font = QtGui.QFont("Calibri", 18)

        pa = reminderLabel.palette()
        pa.setColor(reminderLabel.foregroundRole(), Qt.green)
        reminderLabel.setPalette(pa)
        reminderLabel.setFont(font)
        vMainBox.addWidget(reminderLabel);


        self.displayBusScreen = QtGui.QWidget()
        self.displayBusScreen.setLayout(vMainBox)

    def display(self, text):

        splitMessage = text.split("*")
        screen = splitMessage[0]

        if screen == "init":
            self.setupFirstScreen()
            self.setCentralWidget(self.firstScreen)
        elif screen == "hi":
            self.setupHIScreen()
            self.setCentralWidget(self.secondScreen)
        elif screen.lower() == "screen":
            self.setupChoiceScreen()
            self.setCentralWidget(self.thirdScreen)
        elif screen == "reminder":
            busStopInfo = splitMessage[1]
            self.displayBusStop(busStopInfo)
            self.setCentralWidget(self.displayBusScreen)
        elif screen == "add":
            busStopInfo = splitMessage[1]
            self.displayBusStop(busStopInfo)
            self.setCentralWidget(self.displayBusScreen)
        elif screen == "timing":
            busStopInfo = splitMessage[1]
            self.displayBusStop(busStopInfo, True)
            self.setCentralWidget(self.displayBusScreen)
        elif screen == "destination":
            destinationTest = None
            wait = False
            if len(splitMessage) >= 2:
                destinationTest = splitMessage[1]
            if len(splitMessage) >= 3 :
                wait = splitMessage[2].lower() == "wait"
            self.setupDestination(destinationTest, wait)
            self.setCentralWidget(self.destinationScreen)

    def test(self, text):
        self.workThread = WorkThread(text)
        self.connect(self.workThread, QtCore.SIGNAL("test(QString)"), self.display)
        self.workThread.run()


# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))

    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    client.subscribe("display")



# The callback for when a PUBLISH message is received from the server.

def on_message_ex(ex):
    def on_message( client, userdata, msg):
        print(msg.topic + " " + str(msg.payload))
        payLoad = str(msg.payload)
        ex.test(payLoad)

    return on_message





class WorkThread(QtCore.QThread):
    def __init__(self, text):
        self.text= text
        QtCore.QThread.__init__(self)

    def run(self):
        print "emit signal"
        self.emit(QtCore.SIGNAL("test(QString)"), self.text )
        return

def main():
    client = mqtt.Client()
    app = QtGui.QApplication(sys.argv)
    ex = SplashAwardGUI()

    client.on_connect = on_connect
    client.on_message = on_message_ex(ex)
    client.username_pw_set("splash", "splash")
    client.connect("172.104.58.41", 21, 60, )
    client.loop_start()
    sys.exit(app.exec_())


if __name__ == '__main__':
     main()


pprint.pprint(r.text)
