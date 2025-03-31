let page = 0;
const uuid = '5ccb59cc-b85e-4c9a-8ff7-4d79f238e750';
const apiBase = `http://localhost:8080/api/book-reader/${uuid}`;


const contentEl = document.getElementById("book-content")!;
const pageEl = document.getElementById("page-number")!;
const pageTitleEl = document.createElement("h2"); 

contentEl.insertAdjacentElement("beforebegin", pageTitleEl);

const prevBtn = document.getElementById("prev")!;
const nextBtn = document.getElementById("next")!;


async function loadPage() {
  const res = await fetch(`${apiBase}/${page}`);
  const pages = await res.json();

  if (pages.length === 0) {
    contentEl.innerHTML = "<p>No more pages.</p>";
    pageTitleEl.textContent = "";
    return;
  }

  const currentPage = pages[0];

  pageTitleEl.textContent = currentPage.title ?? "";
  contentEl.innerHTML = currentPage.contentHtml;
  pageEl.textContent = `Page ${currentPage.pageNumber}`;
}

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

loadPage();
