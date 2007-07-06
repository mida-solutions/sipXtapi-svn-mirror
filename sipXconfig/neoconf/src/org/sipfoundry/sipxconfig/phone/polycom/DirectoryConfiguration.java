/*
 * 
 * 
 * Copyright (C) 2006 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2006 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.phone.polycom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.sipfoundry.sipxconfig.device.VelocityProfileGenerator;
import org.sipfoundry.sipxconfig.phonebook.PhonebookEntry;
import org.sipfoundry.sipxconfig.setting.BeanWithSettings;
import org.sipfoundry.sipxconfig.speeddial.Button;
import org.sipfoundry.sipxconfig.speeddial.SpeedDial;

public class DirectoryConfiguration extends VelocityProfileGenerator {
    private Collection<PhonebookEntry> m_entries;
    private List<Button> m_buttons;

    public DirectoryConfiguration(BeanWithSettings phone, Collection<PhonebookEntry> entries,
            SpeedDial speedDial) {
        super(phone);
        m_entries = entries;
        if (speedDial != null) {
            m_buttons = speedDial.getButtons();
        }
    }

    @Override
    protected void addContext(VelocityContext context) {
        super.addContext(context);
    }

    public Collection<PolycomPhonebookEntry> getRows() {
        int size = getSize();
        if (size == 0) {
            return Collections.emptyList();
        }
        Collection<PolycomPhonebookEntry> polycomEntries = new ArrayList<PolycomPhonebookEntry>(
                size);
        if (m_entries != null) {
            transformPhoneBook(m_entries, polycomEntries);
        }
        if (m_buttons != null) {
            transformSpeedDial(m_buttons, polycomEntries);
        }
        return polycomEntries;
    }

    private int getSize() {
        int size = 0;
        if (m_entries != null) {
            size += m_entries.size();
        }
        if (m_buttons != null) {
            size += m_buttons.size();
        }
        return size;
    }

    void transformSpeedDial(List<Button> buttons, Collection<PolycomPhonebookEntry> polycomEntries) {
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            // speed dial entries start with 1 (1..9999)
            polycomEntries.add(new PolycomPhonebookEntry(button, i + 1));
        }
    }

    void transformPhoneBook(Collection<PhonebookEntry> phonebookEntries,
            Collection<PolycomPhonebookEntry> polycomEntries) {
        for (PhonebookEntry entry : phonebookEntries) {
            polycomEntries.add(new PolycomPhonebookEntry(entry));
        }
    }

    public static class PolycomPhonebookEntry {
        private String m_firstName;
        private String m_lastName;
        private String m_contact;
        private int m_speedDial = -1;

        public PolycomPhonebookEntry(PhonebookEntry entry) {
            m_contact = entry.getNumber();
            m_lastName = entry.getLastName();
            m_firstName = entry.getFirstName();
        }

        public PolycomPhonebookEntry(Button button, int speedDial) {
            m_contact = button.getNumber();
            m_firstName = button.getLabel();
            m_speedDial = speedDial;
        }

        public String getFirstName() {
            // username if first and last name are null. Otherwise it creates a
            // contact entry with no display label which is useless on polycom.
            String firstName = m_firstName;
            if (firstName == null && m_lastName == null) {
                return m_contact;
            }
            return firstName;
        }

        public String getLastName() {
            return m_lastName;
        }

        public String getContact() {
            return m_contact;
        }

        public int getSpeedDial() {
            return m_speedDial;
        }
    }
}