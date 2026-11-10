(function () {
    const DATA_KEY = "campusSyncLocalData";
    const SESSION_KEY = "user";
    const ROLE_LOGIN_PAGES = {
        student: "login.html",
        tutor: "tutor-login.html",
        admin: "admin-login.html",
        super_admin: "super-admin-login.html"
    };
    const ROLE_DASHBOARDS = {
        student: "dashboard.html",
        tutor: "tutor.html",
        admin: "admin.html",
        super_admin: "super-admin.html"
    };

    function createId() {
        return window.crypto?.randomUUID
            ? window.crypto.randomUUID()
            : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    }

    function seedData() {
        const now = new Date().toISOString();
        return {
            accounts: [
                { id: "demo-student", email: "student@campussync.local", fullName: "Demo Student", role: "student", password: "Student123!", createdAt: now },
                { id: "demo-tutor", email: "tutor@campussync.local", fullName: "Demo Tutor", role: "tutor", password: "Tutor123!", createdAt: now },
                { id: "demo-admin", email: "admin@campussync.local", fullName: "Demo Admin", role: "admin", password: "Admin123!", createdAt: now },
                { id: "demo-super-admin", email: "superadmin@campussync.local", fullName: "Demo Super Admin", role: "super_admin", password: "SuperAdmin123!", createdAt: now }
            ],
            tasks: [],
            reminders: [],
            assignments: [],
            settings: {},
            notifications: []
        };
    }

    function readData() {
        let value;
        try {
            value = localStorage.getItem(DATA_KEY);
        } catch (error) {
            throw new Error("CampusSync cannot access this browser's local storage.");
        }

        if (!value) {
            const data = seedData();
            writeData(data);
            return data;
        }

        try {
            const data = JSON.parse(value);
            if (!data || !Array.isArray(data.accounts) || !Array.isArray(data.tasks)) {
                throw new Error("Stored CampusSync data has an invalid format.");
            }
            if (!data.accounts.some(account => account.id === "demo-admin" || account.email.toLowerCase() === "admin@campussync.local")) {
                data.accounts.push({
                    id: "demo-admin",
                    email: "admin@campussync.local",
                    fullName: "Demo Admin",
                    role: "admin",
                    password: "Admin123!",
                    createdAt: new Date().toISOString()
                });
                writeData(data);
            }
            data.reminders = Array.isArray(data.reminders) ? data.reminders : [];
            data.assignments = Array.isArray(data.assignments) ? data.assignments : [];
            data.settings = data.settings && typeof data.settings === "object" ? data.settings : {};
            data.notifications = Array.isArray(data.notifications) ? data.notifications : [];
            return data;
        } catch (error) {
            console.error("Could not read CampusSync local data.", error);
            throw new Error("Saved CampusSync data could not be read. Clear this site's local storage to start over.");
        }
    }

    function writeData(data) {
        try {
            localStorage.setItem(DATA_KEY, JSON.stringify(data));
        } catch (error) {
            console.error("Could not save CampusSync local data.", error);
            throw new Error("CampusSync could not save data in this browser.");
        }
    }

    function publicAccount(account) {
        const { password, ...user } = account;
        return user;
    }

    function listAccounts(role) {
        return readData().accounts
            .filter(account => !role || account.role === role)
            .map(publicAccount);
    }

    function getCurrentUser() {
        const cloudUser = window.CampusSyncSession?.user;
        if (cloudUser) return cloudUser;
        try {
            const user = JSON.parse(localStorage.getItem(SESSION_KEY) || "null");
            if (!user || !user.id || !user.role) return null;
            const account = readData().accounts.find(item => item.id === user.id);
            return account && account.accountStatus !== "suspended" ? publicAccount(account) : null;
        } catch (error) {
            console.error("Could not read the current CampusSync account.", error);
            return null;
        }
    }

    function setCurrentUser(account) {
        const user = publicAccount(account);
        localStorage.setItem(SESSION_KEY, JSON.stringify(user));
        localStorage.setItem("token", `local:${account.id}`);
        return user;
    }

    function signIn(email, password, allowedRoles) {
        const normalizedEmail = email.trim().toLowerCase();
        const account = readData().accounts.find(item =>
            item.email.toLowerCase() === normalizedEmail &&
            item.password === password &&
            allowedRoles.includes(item.role)
        );
        if (!account) {
            throw new Error("The email or password is incorrect for this sign-in page.");
        }
        if (account.accountStatus === "suspended") throw new Error("This browser account has been suspended.");
        return setCurrentUser(account);
    }

    function setStudentStatus(studentId, status) {
        if (getCurrentUser()?.role !== "admin" || !["active", "suspended"].includes(status)) {
            throw new Error("Administrator access is required.");
        }
        const data = readData();
        const student = data.accounts.find(account => account.id === studentId && account.role === "student");
        if (!student) throw new Error("Student account could not be found.");
        student.accountStatus = status;
        writeData(data);
    }

    function registerStudent({ fullName, studentNumber, email, password }) {
        const data = readData();
        const normalizedEmail = email.trim().toLowerCase();
        if (data.accounts.some(account => account.email.toLowerCase() === normalizedEmail)) {
            throw new Error("An account with this email already exists.");
        }
        const account = {
            id: createId(),
            email: normalizedEmail,
            fullName: fullName.trim(),
            studentNumber: studentNumber.trim(),
            role: "student",
            password,
            createdAt: new Date().toISOString()
        };
        data.accounts.push(account);
        writeData(data);
        return setCurrentUser(account);
    }

    function generatePassword() {
        return `Campus${Math.random().toString(36).slice(2, 8)}!${Math.floor(10 + Math.random() * 90)}`;
    }

    function createAccount({ fullName, email, role }) {
        if (!["tutor", "admin"].includes(role)) {
            throw new Error("This workspace cannot create that account type.");
        }
        const data = readData();
        const normalizedEmail = email.trim().toLowerCase();
        if (data.accounts.some(account => account.email.toLowerCase() === normalizedEmail)) {
            throw new Error("An account with this email already exists.");
        }
        const account = {
            id: createId(),
            email: normalizedEmail,
            fullName: fullName.trim(),
            role,
            password: generatePassword(),
            createdAt: new Date().toISOString()
        };
        data.accounts.push(account);
        writeData(data);
        return { account: publicAccount(account), temporaryPassword: account.password };
    }

    function assignStudent(tutorId, studentId) {
        const data = readData();
        const tutor = data.accounts.find(account => account.id === tutorId && account.role === "tutor");
        const student = data.accounts.find(account => account.id === studentId && account.role === "student");
        if (!tutor || !student) throw new Error("Choose a valid tutor and student.");
        data.assignments = data.assignments.filter(item => item.studentId !== studentId);
        data.assignments.push({ tutorId, studentId });
        writeData(data);
    }

    function getTasks(userId) {
        return readData().tasks.filter(task => task.userId === userId);
    }

    function addTask(userId, taskInput, reminderAt, assignedBy) {
        const data = readData();
        const task = {
            id: createId(),
            userId,
            title: taskInput.title.trim(),
            description: taskInput.description || "",
            dueDate: taskInput.dueDate || "",
            priority: taskInput.priority || "medium",
            status: "pending",
            createdAt: new Date().toISOString()
        };
        if (assignedBy) task.assignedBy = assignedBy;
        data.tasks.push(task);
        if (reminderAt) {
            data.reminders.push({
                id: createId(),
                userId,
                taskId: task.id,
                message: `Reminder: ${task.title} is due on ${task.dueDate}.`,
                remindAt: reminderAt,
                read: false
            });
        }
        writeData(data);
        return task;
    }

    function tutorStudent(tutorId, studentId) {
        const user = getCurrentUser();
        return user?.id === tutorId && user.role === "tutor" &&
            readData().assignments.some(assignment => assignment.tutorId === tutorId && assignment.studentId === studentId);
    }

    function assignTutorTask(tutorId, studentId, input) {
        if (!tutorStudent(tutorId, studentId)) throw new Error("Choose a student assigned to your account.");
        if (!input.title.trim() || !input.dueDate) throw new Error("Enter a title and due date.");
        return addTask(studentId, input, null, tutorId);
    }

    function reviewTutorTask(tutorId, taskId, status) {
        const task = readData().tasks.find(item => item.id === taskId && item.assignedBy === tutorId);
        if (!task || !tutorStudent(tutorId, task.userId)) throw new Error("That tutor task is unavailable.");
        if (!["pending", "completed"].includes(status)) throw new Error("Choose a valid task status.");
        return updateTask(task.userId, task.id, {status});
    }

    function updateTask(userId, taskId, updates) {
        const data = readData();
        const task = data.tasks.find(item => item.id === taskId && item.userId === userId);
        if (!task) throw new Error("That task could not be found.");
        Object.assign(task, updates);
        writeData(data);
        return task;
    }

    function deleteTask(userId, taskId) {
        const data = readData();
        const before = data.tasks.length;
        data.tasks = data.tasks.filter(item => item.id !== taskId || item.userId !== userId);
        data.reminders = data.reminders.filter(item => item.taskId !== taskId || item.userId !== userId);
        if (before === data.tasks.length) throw new Error("That task could not be found.");
        writeData(data);
    }

    function getSettings(userId) {
        return readData().settings[userId] || {};
    }

    function saveSettings(userId, settings) {
        const data = readData();
        data.settings[userId] = { ...(data.settings[userId] || {}), ...settings };
        writeData(data);
        return data.settings[userId];
    }

    function getNotifications(userId) {
        const data = readData();
        const taskNotifications = data.reminders
            .filter(reminder => reminder.userId === userId)
            .map(reminder => ({
                id: reminder.id,
                title: "Task reminder",
                message: reminder.message,
                type: "task",
                created_at: reminder.remindAt,
                read: Boolean(reminder.read)
            }));
        return [
            ...data.notifications.filter(item => item.userId === userId),
            ...taskNotifications
        ];
    }

    function markNotificationRead(userId, notificationId) {
        const data = readData();
        const notification = data.notifications.find(item =>
            item.id === notificationId && item.userId === userId
        );
        const reminder = data.reminders.find(item =>
            item.id === notificationId && item.userId === userId
        );
        if (notification) notification.read = true;
        if (reminder) reminder.read = true;
        writeData(data);
    }

    function clearNotifications(userId) {
        const data = readData();
        data.notifications = data.notifications.filter(item => item.userId !== userId);
        data.reminders = data.reminders.filter(item => item.userId !== userId);
        writeData(data);
    }

    function changePassword(currentPassword, newPassword) {
        const user = getCurrentUser();
        if (!user) throw new Error("Please sign in again before changing your password.");
        const data = readData();
        const account = data.accounts.find(item => item.id === user.id);
        if (!account || account.password !== currentPassword) {
            throw new Error("The current password is incorrect.");
        }
        account.password = newPassword;
        writeData(data);
    }

    function getAdminOverview() {
        const data = readData();
        const students = data.accounts.filter(account => account.role === "student");
        const tutors = data.accounts.filter(account => account.role === "tutor");
        const tasks = data.tasks;
        const assignmentsByTutor = data.assignments.reduce((result, assignment) => {
            result[assignment.tutorId] = (result[assignment.tutorId] || 0) + 1;
            return result;
        }, {});
        return {
            students: students.map(publicAccount).sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
            tutors: tutors.map(account => ({
                ...publicAccount(account),
                studentCount: assignmentsByTutor[account.id] || 0
            })),
            tasks,
            assignments: data.assignments,
            reminders: data.reminders,
            notifications: data.notifications,
            completedTasks: tasks.filter(task => task.status === "completed").length,
            openTasks: tasks.filter(task => task.status !== "completed").length
        };
    }

    function getSuperAdminOverview() {
        const data = readData();
        const counts = data.accounts.reduce((result, account) => {
            result[account.role] = (result[account.role] || 0) + 1;
            return result;
        }, {});
        return {
            accounts: data.accounts.map(publicAccount),
            totalStudents: counts.student || 0,
            totalTutors: counts.tutor || 0,
            totalAdmins: counts.admin || 0,
            totalSuperAdmins: counts.super_admin || 0,
            tasks: data.tasks
        };
    }

    function getTutorOverview(tutorId) {
        const data = readData();
        const studentIds = data.assignments
            .filter(assignment => assignment.tutorId === tutorId)
            .map(assignment => assignment.studentId);
        const students = data.accounts
            .filter(account => studentIds.includes(account.id) && account.role === "student")
            .map(account => publicAccount(account));
        const tasks = data.tasks.filter(task => studentIds.includes(task.userId));
        const completedTasks = tasks.filter(task => task.status === "completed").length;
        const recentTasks = tasks
            .map(task => ({
                ...task,
                studentName: data.accounts.find(account => account.id === task.userId)?.fullName || "Student"
            }))
            .sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""))
            .slice(0, 8);
        return {
            students: students.map(student => {
                const studentTasks = tasks.filter(task => task.userId === student.id);
                return {
                    ...student,
                    totalTasks: studentTasks.length,
                    completedTasks: studentTasks.filter(task => task.status === "completed").length
                };
            }),
            tasks,
            recentTasks,
            completedTasks,
            completionRate: tasks.length ? Math.round((completedTasks / tasks.length) * 100) : 0
        };
    }

    function removeCurrentUser() {
        localStorage.removeItem("token");
        localStorage.removeItem(SESSION_KEY);
    }

    window.CampusSyncLocal = {
        loginPages: ROLE_LOGIN_PAGES,
        dashboards: ROLE_DASHBOARDS,
        getCurrentUser,
        signIn,
        registerStudent,
        listAccounts,
        createAccount,
        assignStudent,
        setStudentStatus,
        assignTutorTask,
        reviewTutorTask,
        getTasks,
        addTask,
        updateTask,
        deleteTask,
        getSettings,
        saveSettings,
        getNotifications,
        markNotificationRead,
        clearNotifications,
        changePassword,
        getAdminOverview,
        getSuperAdminOverview,
        getTutorOverview,
        removeCurrentUser
    };
})();
