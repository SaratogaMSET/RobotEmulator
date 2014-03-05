/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008-2012. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.command;

import java.util.Enumeration;
import java.util.Vector;

/**
 * A {@link edu.wpi.first.wpilibj.command.CommandGroup} is a list of commands which are executed in sequence.
 * <p/>
 * <p>Commands in a {@link edu.wpi.first.wpilibj.command.CommandGroup} are added using the {@link edu.wpi.first.wpilibj.command.CommandGroup#addSequential(edu.wpi.first.wpilibj.command.Command) addSequential(...)} method
 * and are called sequentially.
 * {@link edu.wpi.first.wpilibj.command.CommandGroup CommandGroups} are themselves {@link edu.wpi.first.wpilibj.command.Command commands}
 * and can be given to other {@link edu.wpi.first.wpilibj.command.CommandGroup CommandGroups}.</p>
 * <p/>
 * <p>{@link edu.wpi.first.wpilibj.command.CommandGroup CommandGroups} will carry all of the requirements of their {@link edu.wpi.first.wpilibj.command.Command subcommands}.  Additional
 * requirements can be specified by calling {@link edu.wpi.first.wpilibj.command.CommandGroup#requires(edu.wpi.first.wpilibj.command.Subsystem) requires(...)}
 * normally in the constructor.</P>
 * <p/>
 * <p>CommandGroups can also execute commands in parallel, simply by adding them
 * using {@link edu.wpi.first.wpilibj.command.CommandGroup#addParallel(edu.wpi.first.wpilibj.command.Command) addParallel(...)}.</p>
 *
 * @author Brad Miller
 * @author Joe Grinstead
 * @see edu.wpi.first.wpilibj.command.Command
 * @see edu.wpi.first.wpilibj.command.Subsystem
 * @see IllegalUseOfCommandException
 */
public class CommandGroup extends Command {

    /**
     * The commands in this group (stored in entries)
     */
    private Vector m_commands = new Vector();
    /**
     * The active children in this group (stored in entries)
     */
    Vector m_children = new Vector();
    /**
     * The current command, -1 signifies that none have been run
     */
    private int m_currentCommandIndex = -1;

    /**
     * Creates a new {@link edu.wpi.first.wpilibj.command.CommandGroup CommandGroup}.
     * The name of this command will be set to its class name.
     */
    public CommandGroup() {
    }

    /**
     * Creates a new {@link edu.wpi.first.wpilibj.command.CommandGroup CommandGroup} with the given name.
     *
     * @param name the name for this command group
     * @throws IllegalArgumentException if name is null
     */
    public CommandGroup(String name) {
        super(name);
    }

    /**
     * Adds a new {@link edu.wpi.first.wpilibj.command.Command Command} to the group.  The {@link edu.wpi.first.wpilibj.command.Command Command} will be started after
     * all the previously added {@link edu.wpi.first.wpilibj.command.Command Commands}.
     * <p/>
     * <p>Note that any requirements the given {@link edu.wpi.first.wpilibj.command.Command Command} has will be added to the
     * group.  For this reason, a {@link edu.wpi.first.wpilibj.command.Command Command's} requirements can not be changed after
     * being added to a group.</p>
     * <p/>
     * <p>It is recommended that this method be called in the constructor.</p>
     *
     * @param command The {@link edu.wpi.first.wpilibj.command.Command Command} to be added
     * @throws IllegalUseOfCommandException if the group has been started before or been given to another group
     * @throws IllegalArgumentException     if command is null
     */
    public synchronized final void addSequential(Command command) {
        validate("Can not add new command to command group");
        if (command == null) {
            throw new IllegalArgumentException("Given null command");
        }

        command.setParent(this);

        m_commands.addElement(new Entry(command, Entry.IN_SEQUENCE));
        for (Enumeration e = command.getRequirements(); e.hasMoreElements(); ) {
            requires((Subsystem) e.nextElement());
        }
    }

    /**
     * Adds a new {@link edu.wpi.first.wpilibj.command.Command Command} to the group with a given timeout.
     * The {@link edu.wpi.first.wpilibj.command.Command Command} will be started after all the previously added commands.
     * <p/>
     * <p>Once the {@link edu.wpi.first.wpilibj.command.Command Command} is started, it will be run until it finishes or the time
     * expires, whichever is sooner.  Note that the given {@link edu.wpi.first.wpilibj.command.Command Command} will have no
     * knowledge that it is on a timer.</p>
     * <p/>
     * <p>Note that any requirements the given {@link edu.wpi.first.wpilibj.command.Command Command} has will be added to the
     * group.  For this reason, a {@link edu.wpi.first.wpilibj.command.Command Command's} requirements can not be changed after
     * being added to a group.</p>
     * <p/>
     * <p>It is recommended that this method be called in the constructor.</p>
     *
     * @param command The {@link edu.wpi.first.wpilibj.command.Command Command} to be added
     * @param timeout The timeout (in seconds)
     * @throws IllegalUseOfCommandException if the group has been started before or been given to another group or
     *                                      if the {@link edu.wpi.first.wpilibj.command.Command Command} has been started before or been given to another group
     * @throws IllegalArgumentException     if command is null or timeout is negative
     */
    public synchronized final void addSequential(Command command, double timeout) {
        validate("Can not add new command to command group");
        if (command == null) {
            throw new IllegalArgumentException("Given null command");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("Can not be given a negative timeout");
        }

        command.setParent(this);

        m_commands.addElement(new Entry(command, Entry.IN_SEQUENCE, timeout));
        for (Enumeration e = command.getRequirements(); e.hasMoreElements(); ) {
            requires((Subsystem) e.nextElement());
        }
    }

    /**
     * Adds a new child {@link edu.wpi.first.wpilibj.command.Command} to the group.  The {@link edu.wpi.first.wpilibj.command.Command} will be started after
     * all the previously added {@link edu.wpi.first.wpilibj.command.Command Commands}.
     * <p/>
     * <p>Instead of waiting for the child to finish, a {@link edu.wpi.first.wpilibj.command.CommandGroup} will have it
     * run at the same time as the subsequent {@link edu.wpi.first.wpilibj.command.Command Commands}.  The child will run until either
     * it finishes, a new child with conflicting requirements is started, or
     * the main sequence runs a {@link edu.wpi.first.wpilibj.command.Command} with conflicting requirements.  In the latter
     * two cases, the child will be canceled even if it says it can't be
     * interrupted.</p>
     * <p/>
     * <p>Note that any requirements the given {@link edu.wpi.first.wpilibj.command.Command Command} has will be added to the
     * group.  For this reason, a {@link edu.wpi.first.wpilibj.command.Command Command's} requirements can not be changed after
     * being added to a group.</p>
     * <p/>
     * <p>It is recommended that this method be called in the constructor.</p>
     *
     * @param command The command to be added
     * @throws IllegalUseOfCommandException if the group has been started before or been given to another command group
     * @throws IllegalArgumentException     if command is null
     */
    public synchronized final void addParallel(Command command) {
        validate("Can not add new command to command group");
        if (command == null) {
            throw new NullPointerException("Given null command");
        }

        command.setParent(this);

        m_commands.addElement(new Entry(command, Entry.BRANCH_CHILD));
        for (Enumeration e = command.getRequirements(); e.hasMoreElements(); ) {
            requires((Subsystem) e.nextElement());
        }
    }

    /**
     * Adds a new child {@link edu.wpi.first.wpilibj.command.Command} to the group with the given timeout.  The {@link edu.wpi.first.wpilibj.command.Command} will be started after
     * all the previously added {@link edu.wpi.first.wpilibj.command.Command Commands}.
     * <p/>
     * <p>Once the {@link edu.wpi.first.wpilibj.command.Command Command} is started, it will run until it finishes, is interrupted,
     * or the time expires, whichever is sooner.  Note that the given {@link edu.wpi.first.wpilibj.command.Command Command} will have no
     * knowledge that it is on a timer.</p>
     * <p/>
     * <p>Instead of waiting for the child to finish, a {@link edu.wpi.first.wpilibj.command.CommandGroup} will have it
     * run at the same time as the subsequent {@link edu.wpi.first.wpilibj.command.Command Commands}.  The child will run until either
     * it finishes, the timeout expires, a new child with conflicting requirements is started, or
     * the main sequence runs a {@link edu.wpi.first.wpilibj.command.Command} with conflicting requirements.  In the latter
     * two cases, the child will be canceled even if it says it can't be
     * interrupted.</p>
     * <p/>
     * <p>Note that any requirements the given {@link edu.wpi.first.wpilibj.command.Command Command} has will be added to the
     * group.  For this reason, a {@link edu.wpi.first.wpilibj.command.Command Command's} requirements can not be changed after
     * being added to a group.</p>
     * <p/>
     * <p>It is recommended that this method be called in the constructor.</p>
     *
     * @param command The command to be added
     * @param timeout The timeout (in seconds)
     * @throws IllegalUseOfCommandException if the group has been started before or been given to another command group
     * @throws IllegalArgumentException     if command is null
     */
    public synchronized final void addParallel(Command command, double timeout) {
        validate("Can not add new command to command group");
        if (command == null) {
            throw new NullPointerException("Given null command");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("Can not be given a negative timeout");
        }

        command.setParent(this);

        m_commands.addElement(new Entry(command, Entry.BRANCH_CHILD, timeout));
        for (Enumeration e = command.getRequirements(); e.hasMoreElements(); ) {
            requires((Subsystem) e.nextElement());
        }
    }

    void _initialize() {
        m_currentCommandIndex = -1;
    }

    void _execute() {
        Entry entry = null;
        Command cmd = null;
        boolean firstRun = false;
        if (m_currentCommandIndex == -1) {
            firstRun = true;
            m_currentCommandIndex = 0;
        }

        while (m_currentCommandIndex < m_commands.size()) {

            if (cmd != null) {
                if (entry.isTimedOut()) {
                    cmd._cancel();
                }
                if (cmd.run()) {
                    break;
                } else {
                    cmd.removed();
                    m_currentCommandIndex++;
                    firstRun = true;
                    cmd = null;
                    continue;
                }
            }

            entry = (Entry) m_commands.elementAt(m_currentCommandIndex);
            cmd = null;

            switch (entry.state) {
                case Entry.IN_SEQUENCE:
                    cmd = entry.command;
                    if (firstRun) {
                        cmd.startRunning();
                        cancelConflicts(cmd);
                    }
                    firstRun = false;
                    break;
                case Entry.BRANCH_PEER:
                    m_currentCommandIndex++;
                    entry.command.start();
                    break;
                case Entry.BRANCH_CHILD:
                    m_currentCommandIndex++;
                    cancelConflicts(entry.command);
                    entry.command.startRunning();
                    m_children.addElement(entry);
                    break;
            }
        }

        // Run Children
        for (int i = 0; i < m_children.size(); i++) {
            entry = (Entry) m_children.elementAt(i);
            Command child = entry.command;
            if (entry.isTimedOut()) {
                child._cancel();
            }
            if (!child.run()) {
                child.removed();
                m_children.removeElementAt(i--);
            }
        }
    }

    void _end() {
        // Theoretically, we don't have to check this, but we do if teams override the isFinished method
        if (m_currentCommandIndex != -1 && m_currentCommandIndex < m_commands.size()) {
            Command cmd = ((Entry) m_commands.elementAt(m_currentCommandIndex)).command;
            cmd._cancel();
            cmd.removed();
        }

        Enumeration children = m_children.elements();
        while (children.hasMoreElements()) {
            Command cmd = ((Entry) children.nextElement()).command;
            cmd._cancel();
            cmd.removed();
        }
        m_children.removeAllElements();
    }

    void _interrupted() {
        _end();
    }

    /**
     * Returns true if all the {@link edu.wpi.first.wpilibj.command.Command Commands} in this group
     * have been started and have finished.
     * <p/>
     * <p>Teams may override this method, although they should probably
     * reference super.isFinished() if they do.</p>
     *
     * @return whether this {@link edu.wpi.first.wpilibj.command.CommandGroup} is finished
     */
    protected boolean isFinished() {
        return m_currentCommandIndex >= m_commands.size() && m_children.isEmpty();
    }

    // Can be overwritten by teams
    protected void initialize() {
    }

    // Can be overwritten by teams
    protected void execute() {
    }

    // Can be overwritten by teams
    protected void end() {
    }

    // Can be overwritten by teams
    protected void interrupted() {
    }

    /**
     * Returns whether or not this group is interruptible.
     * A command group will be uninterruptible if {@link edu.wpi.first.wpilibj.command.CommandGroup#setInterruptible(boolean) setInterruptable(false)}
     * was called or if it is currently running an uninterruptible command
     * or child.
     *
     * @return whether or not this {@link edu.wpi.first.wpilibj.command.CommandGroup} is interruptible.
     */
    public synchronized boolean isInterruptible() {
        if (!super.isInterruptible()) {
            return false;
        }

        if (m_currentCommandIndex != -1 && m_currentCommandIndex < m_commands.size()) {
            Command cmd = ((Entry) m_commands.elementAt(m_currentCommandIndex)).command;
            if (!cmd.isInterruptible()) {
                return false;
            }
        }

        for (int i = 0; i < m_children.size(); i++) {
            if (!((Entry) m_children.elementAt(i)).command.isInterruptible()) {
                return false;
            }
        }

        return true;
    }

    private void cancelConflicts(Command command) {
        for (int i = 0; i < m_children.size(); i++) {
            Command child = ((Entry) m_children.elementAt(i)).command;

            Enumeration requirements = command.getRequirements();

            while (requirements.hasMoreElements()) {
                Object requirement = requirements.nextElement();
                if (child.doesRequire((Subsystem) requirement)) {
                    child._cancel();
                    child.removed();
                    m_children.removeElementAt(i--);
                    break;
                }
            }
        }
    }

    private static class Entry {

        private static final int IN_SEQUENCE = 0;
        private static final int BRANCH_PEER = 1;
        private static final int BRANCH_CHILD = 2;
        Command command;
        int state;
        double timeout;

        Entry(Command command, int state) {
            this.command = command;
            this.state = state;
            this.timeout = -1;
        }

        Entry(Command command, int state, double timeout) {
            this.command = command;
            this.state = state;
            this.timeout = timeout;
        }

        boolean isTimedOut() {
            if (timeout == -1) {
                return false;
            } else {
                double time = command.timeSinceInitialized();
                return time == 0 ? false : time >= timeout;
            }
        }
    }
}
