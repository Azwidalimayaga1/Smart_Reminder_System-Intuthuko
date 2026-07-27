<?php
session_start();
require_once 'includes/db_connection.php';
require_once 'includes/calendar_setup.php';

if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}

$user_id = (int) $_SESSION['user_id'];

if (!isset($_SESSION['role'])) {
    $role_stmt = $conn->prepare("SELECT role FROM Users WHERE user_id = ?");
    $role_stmt->bind_param("i", $user_id);
    $role_stmt->execute();
    $role_result = $role_stmt->get_result();
    $_SESSION['role'] = $role_result->num_rows === 1 ? $role_result->fetch_assoc()['role'] : 'Student';
    $role_stmt->close();
}

$user_role = $_SESSION['role'] ?? 'Student';

if ($user_role === 'Admin') {
    header("Location: admin.php");
    exit();
}

$message = '';
$message_type = '';

// Determine view (month/week)
$view = $_GET['view'] ?? 'month';
$year = intval($_GET['year'] ?? date('Y'));
$month = intval($_GET['month'] ?? date('m'));
$today = date('Y-m-d');

// For weekly view, determine week start
$week_start = $_GET['week_start'] ?? date('Y-m-d', strtotime('sunday this week'));
$week_ts = strtotime($week_start);

// Handle adding event
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['add_event'])) {
    $title = trim($_POST['title'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $event_type = in_array($_POST['event_type'] ?? 'meeting', ['meeting', 'exam', 'workshop', 'deadline'], true) ? $_POST['event_type'] : 'meeting';
    $event_date = trim($_POST['event_date'] ?? '');
    $event_time = trim($_POST['event_time'] ?? '') ?: null;
    $end_date = trim($_POST['end_date'] ?? '') ?: null;
    $end_time = trim($_POST['end_time'] ?? '') ?: null;

    if ($title === '' || $event_date === '') {
        $message = "Event title and date are required.";
        $message_type = 'error';
    } else {
        $stmt = $conn->prepare("
            INSERT INTO CalendarEvents (user_id, title, description, event_type, event_date, event_time, end_date, end_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->bind_param("isssssss", $user_id, $title, $description, $event_type, $event_date, $event_time, $end_date, $end_time);
        $stmt->execute();
        $stmt->close();
        $q = "view=$view";
        if ($view === 'week') { $q .= "&week_start=$week_start"; } else { $q .= "&year=$year&month=$month"; }
        header("Location: calendar.php?$q&msg=added");
        exit();
    }
}

// Handle deleting event
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['delete_event'])) {
    $event_id = intval($_POST['event_id']);
    $stmt = $conn->prepare("DELETE FROM CalendarEvents WHERE event_id = ? AND user_id = ?");
    $stmt->bind_param("ii", $event_id, $user_id);
    $stmt->execute();
    $stmt->close();
    $q = "view=$view";
    if ($view === 'week') { $q .= "&week_start=$week_start"; } else { $q .= "&year=$year&month=$month"; }
    header("Location: calendar.php?$q&msg=deleted");
    exit();
}

// Handle redirect messages
$msg_map = ['added' => ['Event added successfully.', 'success'], 'deleted' => ['Event deleted.', 'success']];
if (isset($_GET['msg'], $msg_map[$_GET['msg']])) {
    $message = $msg_map[$_GET['msg']][0];
    $message_type = $msg_map[$_GET['msg']][1];
}

// Build month data
$first_day = mktime(0, 0, 0, $month, 1, $year);
$days_in_month = date('t', $first_day);
$start_dow = date('w', $first_day);
$prev_month = mktime(0, 0, 0, $month - 1, 1, $year);
$prev_days = date('t', $prev_month);
$month_name = date('F Y', $first_day);

$prev_month_link = $month == 1 ? 12 : $month - 1;
$prev_year_link = $month == 1 ? $year - 1 : $year;
$next_month_link = $month == 12 ? 1 : $month + 1;
$next_year_link = $month == 12 ? $year + 1 : $year;

// Fetch calendar events for the month
$month_start = "$year-$month-01";
$month_end = date('Y-m-t', $first_day);

$events_query = "
    SELECT event_id, title, description, event_type, event_date, event_time, end_date, end_time, color
    FROM CalendarEvents
    WHERE user_id = $user_id
    AND (event_date BETWEEN '$month_start' AND '$month_end'
         OR (end_date IS NOT NULL AND end_date >= '$month_start' AND event_date <= '$month_end'))
    ORDER BY event_date ASC, event_time ASC
";
$events_result = $conn->query($events_query);
$events_by_date = [];
while ($ev = $events_result->fetch_assoc()) {
    $d = $ev['event_date'];
    if (!isset($events_by_date[$d])) $events_by_date[$d] = [];
    $events_by_date[$d][] = $ev;
}

// Fetch task deadlines for the month
$tasks_query = "
    SELECT task_id, title, due_date, priority
    FROM Tasks
    WHERE user_id = $user_id AND status = 'Pending'
    AND DATE(due_date) BETWEEN '$month_start' AND '$month_end'
    ORDER BY due_date ASC
";
$tasks_result = $conn->query($tasks_query);
while ($tk = $tasks_result->fetch_assoc()) {
    $d = date('Y-m-d', strtotime($tk['due_date']));
    if (!isset($events_by_date[$d])) $events_by_date[$d] = [];
    $events_by_date[$d][] = [
        'event_id' => null,
        'title' => $tk['title'],
        'event_type' => 'task-deadline',
        'event_time' => date('H:i:s', strtotime($tk['due_date'])),
        'color' => null,
    ];
}

// Week helpers
function get_week_days($ts) {
    $sunday = strtotime('sunday', $ts);
    $days = [];
    for ($i = 0; $i < 7; $i++) {
        $days[] = date('Y-m-d', strtotime("+$i day", $sunday));
    }
    return $days;
}
$week_days = get_week_days($week_ts);
$week_prev = date('Y-m-d', strtotime('-7 days', $week_ts));
$week_next = date('Y-m-d', strtotime('+7 days', $week_ts));

$day_names = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
$event_icons = [
    'meeting' => 'bi-people-fill',
    'exam' => 'bi-pencil-fill',
    'workshop' => 'bi-tools',
    'deadline' => 'bi-clock-fill',
    'task-deadline' => 'bi-check2-square',
];
$event_colors = [
    'meeting' => '#1e40af',
    'exam' => '#c62828',
    'workshop' => '#2e7d32',
    'deadline' => '#e65100',
    'task-deadline' => '#6a1b9a',
];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Calendar - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body>
    <?php include 'includes/navbar.php'; ?>

    <div class="container">
        <div class="page-header">
            <div>
                <h2>Calendar</h2>
                <p>Track meetings, exams, workshops, and deadlines at a glance</p>
            </div>
            <button class="btn btn-primary" onclick="openModal('addEventModal')"><i class="bi bi-plus-lg"></i> Add Event</button>
        </div>

        <?php if ($message): ?>
            <div class="alert alert-<?php echo $message_type; ?>"><?php echo htmlspecialchars($message); ?></div>
        <?php endif; ?>

        <!-- Legend -->
        <div class="calendar-legend">
            <span class="legend-item"><span class="legend-dot" style="background:#1e40af"></span> Meeting</span>
            <span class="legend-item"><span class="legend-dot" style="background:#c62828"></span> Exam</span>
            <span class="legend-item"><span class="legend-dot" style="background:#2e7d32"></span> Workshop</span>
            <span class="legend-item"><span class="legend-dot" style="background:#e65100"></span> Deadline</span>
            <span class="legend-item"><span class="legend-dot" style="background:#6a1b9a"></span> Task Due</span>
        </div>

        <!-- Navigation -->
        <div class="calendar-nav">
            <div class="calendar-nav-actions">
                <?php if ($view === 'month'): ?>
                    <a href="?view=month&month=<?php echo $prev_month_link; ?>&year=<?php echo $prev_year_link; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-left"></i></a>
                    <h2><?php echo $month_name; ?></h2>
                    <a href="?view=month&month=<?php echo $next_month_link; ?>&year=<?php echo $next_year_link; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-right"></i></a>
                <?php else: ?>
                    <a href="?view=week&week_start=<?php echo $week_prev; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-left"></i></a>
                    <h2><?php echo date('M j', $week_ts) . ' - ' . date('M j, Y', strtotime($week_days[6])); ?></h2>
                    <a href="?view=week&week_start=<?php echo $week_next; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-right"></i></a>
                <?php endif; ?>
            </div>
            <div class="calendar-nav-actions">
                <a href="?view=month" class="btn btn-ghost btn-sm"><i class="bi bi-calendar3"></i> Today</a>
                <div class="view-toggle">
                    <a href="?view=month&year=<?php echo $year; ?>&month=<?php echo $month; ?>" class="view-btn <?php echo $view === 'month' ? 'active' : ''; ?>">Month</a>
                    <a href="?view=week&week_start=<?php echo $today; ?>" class="view-btn <?php echo $view === 'week' ? 'active' : ''; ?>">Week</a>
                </div>
            </div>
        </div>

        <!-- Monthly View -->
        <?php if ($view === 'month'): ?>
        <div class="calendar-grid">
            <?php foreach ($day_names as $dn): ?>
                <div class="calendar-header"><?php echo $dn; ?></div>
            <?php endforeach; ?>

            <?php
            $day = 1;
            $cell_count = $start_dow + $days_in_month;
            $rows = ceil($cell_count / 7);
            $total_cells = $rows * 7;

            for ($i = 0; $i < $total_cells; $i++):
                $is_filler = $i < $start_dow || $day > $days_in_month;
                if ($is_filler && $i < $start_dow) {
                    $pd = $prev_days - $start_dow + $i + 1;
                    $display = $pd;
                    $date_str = date('Y-m-d', mktime(0,0,0,$month-1,$pd,$year));
                } elseif ($is_filler) {
                    $fd = $i - $days_in_month - $start_dow + 1;
                    $display = $fd;
                    $date_str = date('Y-m-d', mktime(0,0,0,$month+1,$fd,$year));
                } else {
                    $display = $day;
                    $date_str = date('Y-m-d', mktime(0,0,0,$month,$day,$year));
                    $day++;
                }
                $cls = $is_filler ? 'calendar-day other-month' : 'calendar-day';
                if ($date_str === $today) $cls .= ' today';
            ?>
                <div class="<?php echo $cls; ?>" onclick="openModal('addEventModal')" data-date="<?php echo $date_str; ?>">
                    <div class="day-number"><?php echo $display; ?></div>
                    <?php if (isset($events_by_date[$date_str])): ?>
                        <?php
                        $shown = 0;
                        $max_visible = 3;
                        foreach ($events_by_date[$date_str] as $ev):
                            if ($shown >= $max_visible) break;
                            $shown++;
                            $et = $ev['event_type'];
                            $icon = $event_icons[$et] ?? 'bi-calendar-event';
                        ?>
                            <div class="calendar-event event-<?php echo $et; ?>"
                                 onclick="event.stopPropagation(); showEventDetail(<?php echo htmlspecialchars(json_encode($ev)); ?>)">
                                <i class="bi <?php echo $icon; ?>"></i>
                                <?php echo htmlspecialchars(mb_strimwidth($ev['title'], 0, 20, '…')); ?>
                            </div>
                        <?php endforeach; ?>
                        <?php if (count($events_by_date[$date_str]) > $max_visible): ?>
                            <div class="event-more">+<?php echo count($events_by_date[$date_str]) - $max_visible; ?> more</div>
                        <?php endif; ?>
                    <?php endif; ?>
                </div>
            <?php endfor; ?>
        </div>

        <!-- Weekly View -->
        <?php else: ?>
        <div class="week-grid">
            <div class="week-header">Time</div>
            <?php foreach ($week_days as $wd): ?>
                <div class="week-header">
                    <?php echo $day_names[date('w', strtotime($wd))]; ?>
                    <span class="date-label"><?php echo date('M j', strtotime($wd)); ?></span>
                </div>
            <?php endforeach; ?>

            <?php
            $slots = [
                '08:00', '09:00', '10:00', '11:00', '12:00',
                '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'
            ];
            foreach ($slots as $slot):
            ?>
                <div class="week-time"><?php echo $slot; ?></div>
                <?php foreach ($week_days as $wd):
                    $day_events = $events_by_date[$wd] ?? [];
                    $slot_events = array_filter($day_events, function($e) use ($slot) {
                        $et = $e['event_time'] ?? '';
                        return $et !== '' && substr($et, 0, 5) === $slot;
                    });
                ?>
                    <div class="week-cell"
                         onclick="openModal('addEventModal')"
                         data-date="<?php echo $wd; ?>"
                         data-time="<?php echo $slot; ?>:00">
                        <?php foreach ($slot_events as $ev):
                            $et = $ev['event_type'];
                            $icon = $event_icons[$et] ?? 'bi-calendar-event';
                        ?>
                            <div class="calendar-event event-<?php echo $et; ?>"
                                 onclick="event.stopPropagation(); showEventDetail(<?php echo htmlspecialchars(json_encode($ev)); ?>)">
                                <i class="bi <?php echo $icon; ?>"></i>
                                <?php echo htmlspecialchars(mb_strimwidth($ev['title'], 0, 16, '…')); ?>
                            </div>
                        <?php endforeach; ?>
                    </div>
                <?php endforeach; ?>
            <?php endforeach; ?>
        </div>
        <?php endif; ?>

        <!-- Add Event Modal -->
        <div id="addEventModal" class="modal">
            <div class="modal-content" style="max-width:540px">
                <span class="close-modal" onclick="closeModal('addEventModal')">&times;</span>
                <h3>Add Event</h3>
                <form method="POST">
                    <div class="event-form-grid">
                        <div class="form-group full-width">
                            <label for="title">Event Title *</label>
                            <input type="text" id="title" name="title" required placeholder="e.g. Midterm Exam">
                        </div>
                        <div class="form-group">
                            <label for="event_type">Type</label>
                            <select id="event_type" name="event_type">
                                <option value="meeting">Meeting</option>
                                <option value="exam">Exam</option>
                                <option value="workshop">Workshop</option>
                                <option value="deadline">Deadline</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="event_date">Date *</label>
                            <input type="date" id="event_date" name="event_date" required>
                        </div>
                        <div class="form-group">
                            <label for="event_time">Start Time</label>
                            <input type="time" id="event_time" name="event_time">
                        </div>
                        <div class="form-group">
                            <label for="end_date">End Date</label>
                            <input type="date" id="end_date" name="end_date">
                        </div>
                        <div class="form-group">
                            <label for="end_time">End Time</label>
                            <input type="time" id="end_time" name="end_time">
                        </div>
                        <div class="form-group full-width">
                            <label for="description">Description</label>
                            <textarea id="description" name="description" rows="3" placeholder="Optional details"></textarea>
                        </div>
                    </div>
                    <button type="submit" name="add_event" class="btn btn-primary btn-block" style="margin-top:8px">Add Event</button>
                </form>
            </div>
        </div>

        <!-- Event Detail Modal -->
        <div id="eventDetailModal" class="modal">
            <div class="modal-content" style="max-width:440px">
                <span class="close-modal" onclick="closeModal('eventDetailModal')">&times;</span>
                <div id="eventDetailContent"></div>
            </div>
        </div>
    </div>

    <script>
    function showEventDetail(event) {
        const types = { meeting: 'Meeting', exam: 'Exam', workshop: 'Workshop', deadline: 'Deadline', 'task-deadline': 'Task Due' };
        const icons = { meeting: 'bi-people-fill', exam: 'bi-pencil-fill', workshop: 'bi-tools', deadline: 'bi-clock-fill', 'task-deadline': 'bi-check2-square' };
        const icon = icons[event.event_type] || 'bi-calendar-event';
        const typeLabel = types[event.event_type] || event.event_type;
        const time = event.event_time ? new Date('1970-01-01T' + event.event_time).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}) : '—';
        const desc = event.description || 'No description.';

        let html = '<div style="text-align:center;margin-bottom:16px">';
        html += '<span style="font-size:40px;color:var(--primary)"><i class="bi ' + icon + '"></i></span>';
        html += '</div>';
        html += '<h3 style="text-align:center;margin-bottom:4px">' + escapeHtml(event.title) + '</h3>';
        html += '<p style="text-align:center;color:var(--gray-600);font-size:13px;margin-bottom:16px">' + typeLabel + '</p>';
        html += '<div style="display:grid;gap:10px">';
        html += '<p><strong>Date:</strong> ' + event.event_date + '</p>';
        html += '<p><strong>Time:</strong> ' + time + '</p>';
        if (event.end_date) html += '<p><strong>End Date:</strong> ' + event.end_date + '</p>';
        html += '<p><strong>Description:</strong> ' + escapeHtml(desc) + '</p>';
        html += '</div>';

        // Delete button for own events (not task deadlines)
        if (event.event_id) {
            html += '<form method="POST" style="margin-top:18px" onsubmit="return confirm(\'Delete this event?\')">';
            html += '<input type="hidden" name="event_id" value="' + event.event_id + '">';
            html += '<button type="submit" name="delete_event" class="btn btn-danger btn-block">Delete Event</button>';
            html += '</form>';
        }

        document.getElementById('eventDetailContent').innerHTML = html;
        openModal('eventDetailModal');
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

    document.addEventListener('DOMContentLoaded', function() {
        var dateInput = document.getElementById('event_date');
        if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];

        document.querySelectorAll('.calendar-day[data-date], .week-cell[data-date]').forEach(function(el) {
            el.addEventListener('click', function() {
                var date = this.getAttribute('data-date');
                var time = this.getAttribute('data-time') || '';
                document.getElementById('event_date').value = date;
                if (time) document.getElementById('event_time').value = time.substring(0,5);
            });
        });
    });
    </script>

    <script src="js/script.js"></script>
</body>
</html>
