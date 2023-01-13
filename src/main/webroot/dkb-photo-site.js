"use strict";

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".categories img, .photos img").forEach((imgEl) => {
    imgEl.parentElement.classList.add("loading");
    imgEl.onload = () => {
        imgEl.parentElement.classList.remove("loading");
    };
  });

  document.body.classList.remove("loading");
});
