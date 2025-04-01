const readerRoot = document.getElementById("reader-root")!;
const sidebar = document.getElementById("reader-sidebar")!;
const contentEl = document.getElementById("book-content")!;
const pageNumberEl = document.getElementById("page-number")!;
const prevBtn = document.getElementById("prev")!;
const nextBtn = document.getElementById("next")!;
const darkToggle = document.getElementById("toggle-dark") as HTMLInputElement;
const toggleSidebar = document.getElementById("toggle-sidebar")!;
const closeSidebarBtn = document.getElementById("close-sidebar")!;
const fontUp = document.getElementById("font-up")!;
const fontDown = document.getElementById("font-down")!;
const fontLabel = document.getElementById("font-size-label")!;

const params = new URLSearchParams(window.location.search);
const uuid = params.get("uuid");

if (!uuid) {
  alert("Missing book UUID");
  window.location.href = "/library.html";
}

let page = 0;
let fontSize = 100;

// 🧠 INIT
init();

async function init() {
  restoreTheme();
  restoreFontSize();
  await getUserProgress();
  await loadPage();
}

// ⚙️ SIDEBAR TOGGLE
toggleSidebar.addEventListener("click", () => {
  readerRoot.classList.add("sidebar-open");
});

closeSidebarBtn.addEventListener("click", () => {
  readerRoot.classList.remove("sidebar-open");
});

// 🌙 THEME TOGGLE
function restoreTheme() {
  const saved = localStorage.getItem("theme");
  if (saved === "dark") {
    readerRoot.classList.add("dark");
    darkToggle.checked = true;
  }

  darkToggle.addEventListener("change", () => {
    const isDark = readerRoot.classList.toggle("dark");
    localStorage.setItem("theme", isDark ? "dark" : "light");
  });
}

// 🔠 FONT ZOOM
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
  fontLabel.textContent = `${value}%`;
  localStorage.setItem("fontSize", value.toString());
}

// ⌨️ KEYBOARD NAVIGATION
document.addEventListener("keydown", (e) => {
  if (e.key === "ArrowLeft") prevBtn.click();
  if (e.key === "ArrowRight") nextBtn.click();
});

// 🔁 PAGE NAVIGATION
prevBtn.addEventListener("click", () => {
  if (page > 0) {
    page--;
    loadPage();
  }
});

nextBtn.addEventListener("click", () => {
  page++;
  loadPage();
});

// 📖 GET PROGRESS FROM BACKEND
async function getUserProgress() {
  const res = await fetch("http://localhost:8080/api/books/all", {
    credentials: "include",
  });

  if (!res.ok) return;

  const books = await res.json();
  const book = books.find((b: any) => b.uuid === uuid);
  page = book?.currentPage || 0;
}

// 📄 LOAD BOOK PAGE CONTENT
async function loadPage() {
  contentEl.classList.remove("show");

  const res = await fetch(`http://localhost:8080/api/book-reader/${uuid}/${page}`, {
    credentials: "include",
  });

  const data = await res.json();

  if (!data.length) {
    contentEl.innerHTML = "<p>No more pages.</p>";
    contentEl.classList.add("show");
    return;
  }

  contentEl.innerHTML = data[0].contentHtml;
  pageNumberEl.textContent = `Page ${data[0].pageNumber}`;

  const isFullImage = contentEl.querySelectorAll("img").length === 1 &&
                      contentEl.children.length === 1;
  contentEl.classList.toggle("full-image-page", isFullImage);

  setTimeout(() => contentEl.classList.add("show"), 20);
}
