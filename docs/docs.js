const node_main_list = document.getElementById("main");
console.log(node_main_list)

const element_menu = document.getElementById("menu")

for (const node of node_main_list.children) {
    const tagName = node.tagName;
    if (tagName.indexOf("H") < 0) continue;
    let html_anchor_element = document.createElement("a");
    html_anchor_element.href = "#" + node.innerText
    const level = tagName.slice(1);
    html_anchor_element.className = 'menu_a menu_item_' + level
    html_anchor_element.innerText = node.innerText
    node.id = node.innerText
    element_menu.appendChild(html_anchor_element)
}