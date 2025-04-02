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
    await getUserProgress();
    await loadPage();
}

toggleSidebar.addEventListener("click", () => {
    readerRoot.classList.add("sidebar-open");
});

closeSidebarBtn.addEventListener("click", () => {
    readerRoot.classList.remove("sidebar-open");
});

closeReaderBtn.addEventListener("click", () => {
    window.location.href = "/library.html";
});

function restoreTheme() {
    const saved = localStorage.getItem("theme");
    if (saved === "dark") {
        readerRoot.classList.add("dark");
        darkToggle.textContent = "🌙";
        darkToggle.checked = true;
    }


    darkToggle.addEventListener("click", () => {
        const isDark = readerRoot.classList.toggle("dark");
        darkToggle.textContent = isDark ?  "🌙" : "☀️";
        localStorage.setItem("theme", isDark ? "dark" : "light");
    });
}

function restoreFontSize() {
    const stored = localStorage.getItem("fontSize");
    if (stored) {
        fontSize = parseInt(stored);
    }
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

document.addEventListener("keydown", (e) => {
    if (e.key === "ArrowLeft") prevBtn.click();
    if (e.key === "ArrowRight") nextBtn.click();
});

prevBtn.addEventListener("click", () => {
    if (page > 0) {
        page--;
        loadPage();
    }
});

nextBtn.addEventListener("click", () => {
    if(!canNext) return;
    page++;
    loadPage();
});

async function getUserProgress() {
    const res = await fetch("http://localhost:8080/api/books/all", {
        credentials: "include",
    });

    if (!res.ok) return;

    const books = await res.json();
    const book = books.find((b: any) => b.uuid === uuid);
    page = book?.currentPage || 0;
}

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
    const pageNumberContent = data[0].pageNumber === 1 ? "Cover" : `Page ${data[0].pageNumber - 1}`;
    pageNumberEl.textContent = pageNumberContent;

    const isFullImage = contentEl.querySelectorAll("img").length === 1 &&
        contentEl.children.length === 1;
    contentEl.classList.toggle("full-image-page", isFullImage);

    setTimeout(() => contentEl.classList.add("show"), 20);
}
