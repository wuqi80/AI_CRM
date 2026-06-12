DELETE FROM manager_role_menu
WHERE menu_id IN (
    SELECT menu_id
    FROM manager_menu
    WHERE realm = 'product:settings'
);

DELETE FROM manager_menu
WHERE realm = 'product:settings'
   OR menu_id = 2910;
