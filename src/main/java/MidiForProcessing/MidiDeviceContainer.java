package MidiForProcessing;

import javax.sound.midi.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Deals with initialization logic related to obtaining a listener for a
 * particular named midi device.
 */
class MidiDeviceContainer {
    private String name;
    private Optional<MidiDevice> midiDevice;
    private ArrayList<Consumer> handlers = new ArrayList<>();

    MidiDeviceContainer(String name) {
        this.name = name;
        midiDevice = locateDevice(name);
    }

    private static MidiDevice unsafeGetMidiDevice(MidiDevice.Info info) {
        try {
            return MidiSystem.getMidiDevice(info);
        } catch (MidiUnavailableException muex) {
            System.out.println("MUEX: " + muex);
            return null;
        }
    }

    private Optional<MidiDevice> locateDevice(String name) {
        /**
         * Locates a Midi device with supplied name.
         * NB: We break the loop as duplicate devices sometimes appear in error
         * on OSX, particularly IAC. There is probably a cause for this but this
         * works.
         */

        Optional<MidiDevice> midiDevice = Stream.of(MidiSystem.getMidiDeviceInfo())
                .filter(x -> x.getName().contains(name))
                .map(MidiDeviceContainer::unsafeGetMidiDevice)
                .findFirst();

        if (midiDevice.isPresent() && !midiDevice.get().isOpen()) {
            try {
                midiDevice.get().open();
            } catch (MidiUnavailableException muex) {
                System.out.println("Unable to open device " + name);
            }
        }

        /**
         * Default listener, prints logging, etc.
         */
        if (midiDevice.isPresent()) {
            try {
                midiDevice.get().getTransmitter().setReceiver(new MidiReceiver(this, true));
            } catch (MidiUnavailableException muex) {
                System.out.println("Midi device unavailable " + name);
            }
        }

        return midiDevice;
    }

    public void connect() {
        this.midiDevice = locateDevice(this.name);
    }

    public boolean hasDevice() {
        return midiDevice.isPresent();
    }

    public Optional<MidiDevice> getMidiDevice() {
        return this.midiDevice;
    }

    public void registerHandler(Consumer<ShortMessage> handler) {
        handlers.add(handler);
    }

    public void receiveMessage(ShortMessage msg) {
        handlers.forEach(x -> x.accept(msg));
    }
}