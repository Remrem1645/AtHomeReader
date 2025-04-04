const readerRoot = document.getElementById("reader-root")!;
const contentEl = document.getElementById("book-content")!;
const pageNumberEl = document.getElementById("page-number")!;
const prevBtn = document.getElementById("prev")!;
const nextBtn = document.getElementById("next")!;
const darkToggle = document.getElementById("toggle-dark") as HTMLInputElement;
const toggleSidebar = document.getElementById("toggle-sidebar")!;
const closeSidebarBtn = document.getElementById("close-sidebar")!;
const fontUp = document.getElementById("font-up")!;
const fontDown = document.getElementById("font-down")!;
const closeReaderBtn = document.getElementById("close-reader")!;
const fontFamilySelect = document.getElementById("font-family-select") as HTMLSelectElement;
const pageInput = document.getElementById("page-input") as HTMLInputElement;
const jumpBtn = document.getElementById("jump-btn")!;

let canNext = true;
const params = new URLSearchParams(window.location.search);
const uuid = params.get("uuid");

if (!uuid) {
    alert("Missing book UUID");
    window.location.href = "/library.html";
}

let page = 0;
let fontSize = 100;

init();

async function init() {
    restoreTheme();
    restoreFontSize();
    restoreFontFamily();
    await getUserProgress();
    await loadPage();
}

// Toggle sidebar
toggleSidebar.addEventListener("click", () => {
    readerRoot.classList.add("sidebar-open");
});

closeSidebarBtn.addEventListener("click", () => {
    readerRoot.classList.remove("sidebar-open");
});

closeReaderBtn.addEventListener("click", () => {
    window.location.href = "/library.html";
});

// Theme toggle
function restoreTheme() {
    const saved = localStorage.getItem("theme");
    if (saved === "dark") {
        readerRoot.classList.add("dark");
        darkToggle.textContent = "🌙";
    }

    darkToggle.addEventListener("click", () => {
        const isDark = readerRoot.classList.toggle("dark");
        darkToggle.textContent = isDark ? "🌙" : "☀️";
        localStorage.setItem("theme", isDark ? "dark" : "light");
    });
}

// Font size controls
function restoreFontSize() {
    const stored = localStorage.getItem("fontSize");
    if (stored) fontSize = parseInt(stored);
    applyFontSize(fontSize);

    fontUp.addEventListener("click", () => {
        if (fontSize < 200) {
            fontSize += 10;
            applyFontSize(fontSize);
        }
    });

    fontDown.addEventListener("click", () => {
        if (fontSize > 50) {
            fontSize -= 10;
            applyFontSize(fontSize);
        }
    });
}

function applyFontSize(value: number) {
    fontSize = value;
    readerRoot.style.setProperty("--font-size", value.toString());
    localStorage.setItem("fontSize", value.toString());
}

// Font family selection
function restoreFontFamily() {
    const savedFont = localStorage.getItem("fontFamily") || "serif";
    readerRoot.style.setProperty("--font-family", savedFont);
    fontFamilySelect.value = savedFont;

    fontFamilySelect.addEventListener("change", () => {
        const selectedFont = fontFamilySelect.value;
        readerRoot.style.setProperty("--font-family", selectedFont);
        localStorage.setItem("fontFamily", selectedFont);
    });
}

// Keyboard navigation
document.addEventListener("keydown", (e) => {
    if (e.key === "ArrowLeft") prevBtn.click();
    if (e.key === "ArrowRight") nextBtn.click();
});

// Page navigation buttons
prevBtn.addEventListener("click", () => {
    if (page > 0) {
        page--;
        loadPage();
    }
});

nextBtn.addEventListener("click", () => {
    if (!canNext) return;
    page++;
    loadPage();
});

// Jump to page feature
jumpBtn.addEventListener("click", () => {
    const jumpPage = parseInt(pageInput.value);
    if (!isNaN(jumpPage) && jumpPage >= 0) {
        page = jumpPage;
        loadPage();
    } else {
        alert("Enter a valid page number.");
    }
});

// Load user progress
async function getUserProgress() {
    const res = await fetch("http://localhost:8080/api/books/all", { credentials: "include" });
    if (!res.ok) return;

    const books = await res.json();
    const book = books.find((b: any) => b.uuid === uuid);
    page = book?.currentPage || 0;
}

// Load page content
async function loadPage() {
    contentEl.classList.remove("show");

    const res = await fetch(`http://localhost:8080/api/book-reader/${uuid}/${page}`, {
        credentials: "include",
    });

    const data = await res.json();

    if (!data.length) {
        contentEl.innerHTML = "<p>No more pages.</p>";
        contentEl.classList.add("show");
        nextBtn.style.display = "none";
        pageNumberEl.textContent = "End";
        canNext = false;
        return;
    }

    canNext = true;
    nextBtn.style.display = "block";
    contentEl.innerHTML = data[0].content;
    const pageNumberContent = data[0].pageNumber === 0 ? "Cover" : `Page ${data[0].pageNumber}`;
    pageNumberEl.textContent = pageNumberContent;

    const isFullImage = contentEl.querySelectorAll("img").length === 1 && contentEl.children.length === 1;
    contentEl.classList.toggle("full-image-page", isFullImage);

    setTimeout(() => contentEl.classList.add("show"), 20);
}
