/**
 * Smart Reminder System - JavaScript Functions
 * Intuthuko College Staff Portal
 */

// ============================================
// AUTHENTICATION PAGE FUNCTIONS
// ============================================

function showTab(tabName, btn) {
    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => content.classList.remove('active'));

    const tabButtons = document.querySelectorAll('.tab-btn');
    tabButtons.forEach(button => button.classList.remove('active'));

    const selectedTab = document.getElementById(tabName);
    if (selectedTab) selectedTab.classList.add('active');

    if (btn) btn.classList.add('active');
}

/**
 * Toggle password field visibility
 * @param {string} inputId - ID of the password input
 * @param {HTMLElement} btn - The toggle button
 */
function togglePassword(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const isPassword = input.type === 'password';
    input.type = isPassword ? 'text' : 'password';
    btn.textContent = isPassword ? 'Hide' : 'Show';
    btn.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
}

/**
 * Update password strength indicator on register page
 */
function updatePasswordStrength() {
    const password = document.getElementById('password');
    const strengthBar = document.getElementById('passwordStrength');
    const strengthText = document.getElementById('passwordStrengthText');
    if (!password || !strengthBar) return;

    const val = password.value;
    let score = 0;
    let label = 'Weak';
    let pct = 0;

    if (val.length >= 6) score++;
    if (val.length >= 10) score++;
    if (/[a-z]/.test(val) && /[A-Z]/.test(val)) score++;
    if (/\d/.test(val)) score++;
    if (/[^a-zA-Z0-9]/.test(val)) score++;

    if (score <= 1) { label = 'Weak'; pct = 20; }
    else if (score === 2) { label = 'Fair'; pct = 40; }
    else if (score === 3) { label = 'Good'; pct = 60; }
    else if (score === 4) { label = 'Strong'; pct = 80; }
    else { label = 'Very Strong'; pct = 100; }

    strengthBar.style.width = pct + '%';
    strengthBar.className = 'strength-bar-fill';
    if (pct <= 20) strengthBar.classList.add('strength-weak');
    else if (pct <= 40) strengthBar.classList.add('strength-fair');
    else if (pct <= 60) strengthBar.classList.add('strength-good');
    else if (pct <= 80) strengthBar.classList.add('strength-strong');
    else strengthBar.classList.add('strength-very-strong');

    if (strengthText) strengthText.textContent = val.length > 0 ? label : '';
}

// ============================================
// MODAL FUNCTIONS
// ============================================

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
}

function closeActiveModal() {
    const activeModal = document.querySelector('.modal.active');
    if (activeModal) {
        closeModal(activeModal.id);
    }
}

// Close modal on overlay click or Escape key
document.addEventListener('click', function (e) {
    if (e.target.classList.contains('modal')) {
        closeModal(e.target.id);
    }
});

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeActiveModal();
});

// ============================================
// LOADING STATE
// ============================================

function showLoading(btn) {
    if (!btn || btn.classList.contains('btn-loading')) return;
    btn.classList.add('btn-loading');
}

document.addEventListener('submit', function (e) {
    const btn = e.target.querySelector('.btn-loading-submit');
    if (btn) showLoading(btn);
});

// ============================================
// HAMBURGER MENU (NAVBAR)
// ============================================

function toggleMobileMenu() {
    const menu = document.getElementById('navbarMenu');
    const hamburger = document.querySelector('.hamburger');
    if (menu && hamburger) {
        menu.classList.toggle('active');
        hamburger.classList.toggle('active');
    }
}

// Close mobile menu on nav link click
document.addEventListener('click', function (e) {
    const menu = document.getElementById('navbarMenu');
    const hamburger = document.querySelector('.hamburger');
    if (menu && menu.classList.contains('active') && !e.target.closest('.navbar-container')) {
        menu.classList.remove('active');
        if (hamburger) hamburger.classList.remove('active');
    }
});

// ============================================
// SMOOTH SCROLL
// ============================================

document.addEventListener('click', function (e) {
    const link = e.target.closest('a[href^="#"]');
    if (link) {
        const target = document.querySelector(link.getAttribute('href'));
        if (target) {
            e.preventDefault();
            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }
});

// ============================================
// TASK MANAGEMENT FUNCTIONS
// ============================================

function deleteTask(taskId) {
    if (confirm('Are you sure you want to delete this task? This action cannot be undone.')) {
        const form = document.getElementById('delete-form-' + taskId);
        if (form) form.submit();
    }
}

/**
 * Edit task (opens modal with pre-filled data)
 * @param {number} taskId - ID of the task to edit
 * @param {string} title - Task title
 * @param {string} description - Task description
 * @param {string} dueDate - Task due date
 * @param {string} priority - Task priority
 */
function editTask(taskId, title, description, dueDate, priority) {
    // Populate form fields
    document.getElementById('edit_task_id').value = taskId;
    document.getElementById('edit_title').value = title;
    document.getElementById('edit_description').value = description;
    document.getElementById('edit_due_date').value = dueDate;
    document.getElementById('edit_priority').value = priority;

    // Open modal
    openModal('editTaskModal');
}

/**
 * Mark task as completed
 * @param {number} taskId - ID of the task to complete
 */
function completeTask(taskId) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '';

    const taskInput = document.createElement('input');
    taskInput.type = 'hidden';
    taskInput.name = 'task_id';
    taskInput.value = taskId;

    const statusInput = document.createElement('input');
    statusInput.type = 'hidden';
    statusInput.name = 'status';
    statusInput.value = 'Completed';

    const updateInput = document.createElement('input');
    updateInput.type = 'hidden';
    updateInput.name = 'update_status';

    form.appendChild(taskInput);
    form.appendChild(statusInput);
    form.appendChild(updateInput);
    document.body.appendChild(form);
    form.submit();
}

// ============================================
// FORM VALIDATION
// ============================================

/**
 * Validate email format
 * @param {string} email - Email to validate
 * @returns {boolean} - True if email is valid
 */
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validate password strength
 * @param {string} password - Password to validate
 * @returns {object} - Object with strength level and feedback
 */
function validatePasswordStrength(password) {
    let strength = 'Weak';
    let feedback = [];

    if (password.length >= 8) {
        strength = 'Medium';
    }
    if (password.match(/[a-z]/) && password.match(/[A-Z]/)) {
        strength = 'Strong';
    }
    if (password.match(/[0-9]/) && password.match(/[^a-zA-Z0-9]/)) {
        strength = 'Very Strong';
    }

    if (password.length < 6) {
        feedback.push('Password must be at least 6 characters long');
    }
    if (!password.match(/[A-Z]/)) {
        feedback.push('Add uppercase letters for stronger password');
    }
    if (!password.match(/[0-9]/)) {
        feedback.push('Add numbers for stronger password');
    }

    return { strength, feedback };
}

// ============================================
// DATETIME UTILITIES
// ============================================

/**
 * Get human-readable time difference
 * @param {string} dateString - Date string to compare
 * @returns {string} - Human-readable time difference
 */
function getTimeRemaining(dateString) {
    const dueDate = new Date(dateString);
    const now = new Date();
    const diff = dueDate - now;

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    if (diff < 0) {
        return 'Overdue by ' + Math.abs(days) + ' day(s)';
    } else if (days > 0) {
        return days + ' day(s) remaining';
    } else if (hours > 0) {
        return hours + ' hour(s) remaining';
    } else {
        return minutes + ' minute(s) remaining';
    }
}

/**
 * Format date for display
 * @param {string} dateString - Date string to format
 * @returns {string} - Formatted date
 */
function formatDate(dateString) {
    const options = { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: true 
    };
    return new Date(dateString).toLocaleDateString('en-US', options);
}

// ============================================
// NOTIFICATION FUNCTIONS
// ============================================

/**
 * Show notification toast
 * @param {string} message - Message to display
 * @param {string} type - Type of notification (success, error, info, warning)
 * @param {number} duration - Duration in milliseconds
 */
function showNotification(message, type = 'info', duration = 3000) {
    // Create toast element
    const toast = document.createElement('div');
    toast.className = 'notification notification-' + type;
    toast.innerHTML = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${getNotificationColor(type)};
        color: white;
        padding: 15px 20px;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        animation: slideIn 0.3s ease-out;
        max-width: 400px;
    `;

    // Add to body
    document.body.appendChild(toast);

    // Remove after duration
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease-out';
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 300);
    }, duration);
}

/**
 * Get color for notification type
 * @param {string} type - Type of notification
 * @returns {string} - Color code
 */
function getNotificationColor(type) {
    const colors = {
        'success': '#28a745',
        'error': '#dc3545',
        'warning': '#ffc107',
        'info': '#0055cc'
    };
    return colors[type] || colors['info'];
}

// ============================================
// SEARCH & FILTER FUNCTIONS
// ============================================

/**
 * Filter tasks by status
 * @param {string} status - Status to filter by (Pending, Completed)
 */
function filterTasksByStatus(status) {
    const rows = document.querySelectorAll('.task-row');
    
    rows.forEach(row => {
        if (status === 'All') {
            row.style.display = '';
        } else {
            if (row.classList.contains('status-' + status.toLowerCase())) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        }
    });
}

/**
 * Filter tasks by priority
 * @param {string} priority - Priority level to filter by
 */
function filterTasksByPriority(priority) {
    const rows = document.querySelectorAll('.task-row');
    
    rows.forEach(row => {
        if (priority === 'All') {
            row.style.display = '';
        } else {
            if (row.textContent.includes(priority)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        }
    });
}

/**
 * Search tasks by title or description
 * @param {string} query - Search query
 */
function searchTasks(query) {
    const rows = document.querySelectorAll('.task-row');
    const searchTerm = query.toLowerCase();
    
    rows.forEach(row => {
        const title = row.querySelector('.task-title').textContent.toLowerCase();
        const description = row.querySelector('.task-desc').textContent.toLowerCase();
        
        if (title.includes(searchTerm) || description.includes(searchTerm)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

// ============================================
// AUTO-REFRESH FUNCTIONS
// ============================================

/**
 * Auto-refresh reminders every 5 minutes
 */
function setupAutoRefresh() {
    // Check if we're on the reminders page
    if (document.querySelector('.reminders-section')) {
        // Refresh every 5 minutes (300000 milliseconds)
        setInterval(() => {
            // Optional: Add refresh logic here
            console.log('Reminder check triggered at ' + new Date().toLocaleTimeString());
        }, 300000);
    }
}

function setupRoleSubjectToggle() {
    const roleSelect = document.getElementById('roleSelect');
    const roleInputs = document.querySelectorAll('input[name="role"]');
    const panels = {
        Student: document.getElementById('studentRolePanel'),
        Tutor: document.getElementById('tutorRolePanel'),
        Admin: document.getElementById('adminRolePanel')
    };

    if (!roleSelect && roleInputs.length === 0) {
        return;
    }

    const getSelectedRole = () => {
        if (roleSelect) {
            return roleSelect.value;
        }

        const selectedRole = document.querySelector('input[name="role"]:checked');
        return selectedRole ? selectedRole.value : 'Student';
    };

    const updateSubjectVisibility = () => {
        const selectedRole = getSelectedRole();

        Object.keys(panels).forEach((role) => {
            if (panels[role]) {
                panels[role].classList.toggle('active', role === selectedRole);
            }
        });
    };

    if (roleSelect) {
        roleSelect.addEventListener('change', updateSubjectVisibility);
    }

    roleInputs.forEach((input) => {
        input.addEventListener('change', updateSubjectVisibility);
    });

    updateSubjectVisibility();
}

// ============================================
// INITIALIZATION
// ============================================

/**
 * Initialize page on load
 */
document.addEventListener('DOMContentLoaded', function() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    if (tabButtons.length > 0) {
        tabButtons[0].classList.add('active');
        const firstTab = document.querySelector('.tab-content');
        if (firstTab) firstTab.classList.add('active');
    }

    setupAutoRefresh();
    setupRoleSubjectToggle();

    // Password strength indicator
    const pwInput = document.getElementById('password');
    if (pwInput && document.getElementById('passwordStrength')) {
        pwInput.addEventListener('input', updatePasswordStrength);
    }

    // Active nav link
    const currentPath = window.location.pathname.split('/').pop();
    document.querySelectorAll('.nav-link').forEach(function (link) {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('nav-link-active');
        }
    });

});

// ============================================
// UTILITY FUNCTIONS
// ============================================

/**
 * Clear all form fields
 * @param {HTMLElement} form - Form element to clear
 */
function clearForm(form) {
    if (form) {
        form.reset();
    }
}

/**
 * Copy text to clipboard
 * @param {string} text - Text to copy
 */
function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showNotification('Copied to clipboard!', 'success', 2000);
    }).catch(() => {
        showNotification('Failed to copy', 'error', 2000);
    });
}

/**
 * Export data to CSV
 * @param {array} data - Data to export
 * @param {string} filename - Output filename
 */
function exportToCSV(data, filename = 'export.csv') {
    let csv = '';
    
    if (data.length > 0) {
        // Get headers
        const headers = Object.keys(data[0]);
        csv = headers.join(',') + '\n';
        
        // Get data
        data.forEach(row => {
            const values = headers.map(header => {
                const value = row[header];
                // Escape quotes and wrap in quotes if contains comma
                return typeof value === 'string' && value.includes(',') 
                    ? '"' + value.replace(/"/g, '""') + '"' 
                    : value;
            });
            csv += values.join(',') + '\n';
        });
    }
    
    // Create blob and download
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
}
