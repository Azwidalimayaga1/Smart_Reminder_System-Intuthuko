// JS/gpa.js

const gradeScale = [
    { min: 75, grade: 'PD', points: 4.0 },
    { min: 70, grade: 'P', points: 4.0 },
    { min: 65, grade: 'F', points: 0.0 },
    { min: 60, grade: 'FES', points: 0.0 },
    { min: 55, grade: 'IC', points: 4.0 },
    { min: 50, grade: 'EC', points: 4.0 }
];

function marksToGrade(marks) {
    const m = parseFloat(marks);
    if (isNaN(m)) return { grade: '-', points: null };
    const found = gradeScale.find(g => m >= g.min);
    return found ? found : { grade: '-', points: null };
}

const gpaTable = document.getElementById('gpaTable');
const addRowBtn = document.getElementById('addRowBtn');
const form = document.getElementById('gpa-form');
const gpaResult = document.getElementById('gpaResult');
const gpaValue = document.getElementById('gpaValue');

function createRow() {
    const row = document.createElement('div');
    row.className = 'gpa-row';

    row.innerHTML = `
        <input type="text" class="course-input" placeholder="e.g. Math 101" required>
        <input type="number" class="marks-input" placeholder="e.g. 85" min="0" max="100" required>
        <div class="grade-display">-</div>
        <input type="number" class="credit-input" placeholder="e.g. 3" min="0" required>
        <button type="button" class="remove-row-btn" title="Remove course">&times;</button>
    `;

    const marksInput = row.querySelector('.marks-input');
    const gradeDisplay = row.querySelector('.grade-display');
    marksInput.addEventListener('input', () => {
        gradeDisplay.textContent = marksToGrade(marksInput.value).grade;
    });

    row.querySelector('.remove-row-btn').addEventListener('click', () => row.remove());

    gpaTable.appendChild(row);
}

// start with 3 rows
for (let i = 0; i < 3; i++) createRow();

addRowBtn.addEventListener('click', createRow);

form.addEventListener('submit', (e) => {
    e.preventDefault();

    const rows = gpaTable.querySelectorAll('.gpa-row:not(.gpa-row-header)');
    let totalPoints = 0;
    let totalCredits = 0;
    let hasValidRow = false;

    rows.forEach(row => {
        const marks = row.querySelector('.marks-input').value;
        const credit = parseFloat(row.querySelector('.credit-input').value);
        const { points } = marksToGrade(marks);

        if (points !== null && !isNaN(credit) && credit > 0) {
            totalPoints += points * credit;
            totalCredits += credit;
            hasValidRow = true;
        }
    });

    if (!hasValidRow) {
        alert('Please enter valid marks and credits for at least one course.');
        return;
    }

    const gpa = totalPoints / totalCredits;
    gpaValue.textContent = gpa.toFixed(2);
    gpaResult.style.display = 'block';
});

form.addEventListener('reset', () => {
    gpaTable.querySelectorAll('.gpa-row:not(.gpa-row-header)').forEach(row => row.remove());
    gpaResult.style.display = 'none';
    for (let i = 0; i < 3; i++) createRow();
});