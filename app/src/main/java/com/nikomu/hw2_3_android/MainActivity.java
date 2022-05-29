/**
 * 2. Необходимо создать приложение «Файловый менеджер». Внешний вид приложения изображен на
 * рис. Приложение «Файловый менеджер» работает только с файлами и каталогами, находящимися на
 * внешнем носителе. Файлы и каталоги отображаются в списке android.widget.GridView. Попробуйте
 * реализовать custom-подсветку выбранного пользователем элемента.
 * При длительном нажатии на каталог, приложение «Файловый менеджер» «заходит» в этот каталог
 * и отображает содержимое этого каталога. Для обработки события длительного нажатия необходимо
 * использовать метод класса android.widget.GridView:
 * В приложении «Файловый менеджер» используется три вида изображений: для каталогов, для
 * текстовых файлов и для всех остальных типов файлов.
 * Пользователь может совершать с помощью приложения «Файловый менеджер» следующие действия:
 * создание каталога, удаление файла или каталога (каталог удаляется рекурсивно), переименование
 * файла или каталога, копирование и вставка файла или каталога (каталог копируется рекурсивно).
 * Для выбора соответствующего действия необходимо использовать меню или контекстное меню
 * (на ваш выбор).
 * Перед удалением файла или каталога необходимо с помощью Диалогового окна уточнить у
 * пользователя подтверждение операции удаления.
 * Приложение «Файловый менеджер» не удаляет и не переименовывает общедоступные каталоги.
 */

package com.nikomu.hw2_3_android;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    /**
     * Шлях до кореневого каталогу на зовнішньому носію інформації
     */
    private final String ROOT_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath();

    /**
     * Поточна директорія
     */
    private File currentDirectory;

    /**
     * Індекс вибраного елемента списку
     */
    private int currentItem = -1;

    /**
     * Посилання на віджет поточного вибраного елемента списку
     */
    private View currentView;

    /**
     * Список іконок до файлів та католога
     */
    private ArrayList<Bitmap> iconsList;

    MenuItem miCreateDirectory;
    MenuItem miDelete;
    MenuItem miRename;
    MenuItem miCopy;
    MenuItem miPaste;
    GridView gvDirectoriesAndFiles;
    Button btnGoPrevious;
    Button btnGoHome;
    TextView tvPath;
    TextView tvTitleDialog;

    /**
     * Елемент GridView
     */
    GridViewItem item;

    private File currentFile;

    /**
     * Колір фону невибраного елемента списку
     */
    private int defaultBgColor = Color.rgb(255, 255, 255);

    /**
     * Колір фону вибраного елемента списку
     */
    private int selectBgColor = Color.rgb(255, 99, 71);

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isPermission();
        initViews();
        iconsList = getIcons();
        createMainActivity(ROOT_DIRECTORY);
        initListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.miCreateDirectory:
                createDirectoryAction();
                break;
            case R.id.miDelete:
                deleteFileOrDirectoryAction();
                break;
            case R.id.miRename:
                renameAction();
                break;
            case R.id.miCopy:
                copyAction();
                break;
            case R.id.miPaste:
                pasteAction();
                break;
        }
        return true;
    }

    /**
     * Метод для початкової ініціалізації видів (views)
     */
    private void initViews() {
        miCreateDirectory = this.findViewById(R.id.miCreateDirectory);
        miDelete = this.findViewById(R.id.miDelete);
        miRename = this.findViewById(R.id.miRename);
        miCopy = this.findViewById(R.id.miCopy);
        miPaste = this.findViewById(R.id.miPaste);
        btnGoPrevious = this.findViewById(R.id.btnGoPrevious);
        btnGoHome = this.findViewById(R.id.btnGoHome);
        gvDirectoriesAndFiles = this.findViewById(R.id.gvDirectoriesAndFiles);
        tvPath = this.findViewById(R.id.tvPath);
        tvTitleDialog = this.findViewById(R.id.tvTitle);
    }

    public boolean isPermission() {
        int permission = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = checkSelfPermission(WRITE_EXTERNAL_STORAGE);
        }

        if (permission != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

            return true;
        }

        return false;
    }

    /**
     * Метод для заповнення головної активності (activity) згідно поточного каталогу.
     *
     * @param directoryName - поточний каталог.
     */
    private void createMainActivity(String directoryName) {
        currentDirectory = new File(directoryName);

        tvPath.setText(currentDirectory.getAbsolutePath());

        ArrayList<GridViewItem> items = fillDirectory(currentDirectory);

        ArrayAdapter<GridViewItem> adapter = new ArrayAdapter<GridViewItem>(this, R.layout.grid_view_item, R.id.tvTitle, items) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                GridViewItem gridViewItem = this.getItem(position);

                ImageView ivImg = view.findViewById(R.id.ivImg);
                ivImg.setImageBitmap(gridViewItem.getImage());

                TextView tvTitle = view.findViewById(R.id.tvTitle);
                tvTitle.setText(gridViewItem.getTitle());

                if (position == currentItem) {
                    view.setBackgroundColor(selectBgColor);
                    currentView = view;
                } else {
                    view.setBackgroundColor(MainActivity.this.defaultBgColor);
                }

                return view;
            }
        };

        gvDirectoriesAndFiles.setAdapter(adapter);
    }

    /**
     * Метод для початкової ініціалізації прослуховувачів (listeners)
     */
    private void initListeners() {
        gvDirectoriesAndFiles.setOnItemClickListener((parent, view, position, id) -> {
            item = (GridViewItem) parent.getAdapter().getItem(position);

            if (currentItem != -1) {
                currentView.setBackgroundColor(defaultBgColor);
            }

            currentItem = position;
            currentView = view;
            currentView.setBackgroundColor(selectBgColor);
        });

        gvDirectoriesAndFiles.setOnItemLongClickListener((parent, view, position, id) -> {
            TextView tvTitle = view.findViewById(R.id.tvTitle);
            String dirPath = tvPath.getText().toString() + "/" + tvTitle.getText().toString();
            File file = new File(dirPath);

            if (file.isDirectory()) {
                createMainActivity(dirPath);
            } else {
                Toast.makeText(this, "Оберіть каталог, а не файл", Toast.LENGTH_SHORT).show();
            }

            return true;
        });

        btnGoHome.setOnClickListener(view -> createMainActivity(ROOT_DIRECTORY));

        btnGoPrevious.setOnClickListener(view -> {
            String path = tvPath.getText().toString();
            if (path.equals(ROOT_DIRECTORY) || path.length() <= ROOT_DIRECTORY.length()) {
                createMainActivity(ROOT_DIRECTORY);
            } else {
                int index = path.lastIndexOf("/");
                createMainActivity(path.substring(0, index));
            }
        });
    }

    /**
     * Метод для отримання списку зображень
     *
     * @return ArrayList<Bitmap>
     */
    private ArrayList<Bitmap> getIcons() {
        ArrayList<Bitmap> icons = new ArrayList<>();
        AssetManager assetManager = this.getAssets();

        try {
            String[] assets = assetManager.list("");
            for (String asset : assets) {
                try {
                    InputStream inputStream = assetManager.open(asset);
                    Bitmap bmp = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    icons.add(bmp);
                } catch (Exception e) {
                }
            }
        } catch (IOException ioe) {
        }

        return icons;
    }

    /**
     * Метод проверяет готовность внешнего носителя для операций чтения/записи.
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (state.equals(Environment.MEDIA_MOUNTED));
    }

    /**
     * Метод перевіряє готовність зовнішнього носія для операцій читання даних
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (state.equals(Environment.MEDIA_MOUNTED) || state.equals(Environment.MEDIA_MOUNTED_READ_ONLY));
    }

    /**
     * Метод повертає список назв файлів та каталогів зовнішнього носія, які знаходяться у вказаному каталозі.
     *
     * @param file - Каталог зовнішнього носія, список файлів та підкаталогів якого необхідно отримати.
     * @return Колекцію любого будь-якого розміру строк.
     */
    private ArrayList<GridViewItem> fillDirectory(File file) {
        ArrayList<GridViewItem> listFiles = new ArrayList<>();

        if (this.isExternalStorageReadable()) {
            File[] arrFiles = file.listFiles();

            if (arrFiles != null) {
                for (File currentFile : arrFiles) {
                    if (currentFile.isDirectory()) {
                        listFiles.add(new GridViewItem(iconsList.get(FileTypes.DIRECTORY.ordinal()),
                                currentFile.getName(), FileTypes.DIRECTORY.ordinal()));
                    } else if (isTextFile(currentFile)) {
                        listFiles.add(new GridViewItem(iconsList.get(FileTypes.TEXT_FILE.ordinal()),
                                currentFile.getName(), FileTypes.TEXT_FILE.ordinal()));
                    } else {
                        listFiles.add(new GridViewItem(iconsList.get(FileTypes.OTHER_FILE.ordinal()),
                                currentFile.getName(), FileTypes.OTHER_FILE.ordinal()));
                    }
                }
            }
        }

        return listFiles;
    }

    /**
     * Метод який здійснює операцію створення нового каталогу
     */
    private void createDirectoryAction() {
        if (/*isPermission() &&*/ isExternalStorageWritable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog_Alert);
            builder.setTitle("Створення директорії");

            LayoutInflater inflater = this.getLayoutInflater();
            final View view = inflater.inflate(R.layout.activity_dialog, null, false);
            EditText etFileNameDialog = view.findViewById(R.id.etFileNameDialog);

            builder.setView(view);

            builder.setPositiveButton("Створити", (dialog, which) -> {
                String filePath = currentDirectory.getPath() + "/" + etFileNameDialog.getText().toString();
                File newDirectory = new File(filePath);

                boolean createResult = newDirectory.mkdir();

                if (createResult) {
                    Toast.makeText(this, "Каталог [" + etFileNameDialog.getText().toString() + "] упішно сторено!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Помилка створення каталогу [" + etFileNameDialog.getText().toString() + "]!", Toast.LENGTH_SHORT).show();
                }

                createMainActivity(currentDirectory.getAbsolutePath());
            });

            builder.setNegativeButton("Відмінити", ((dialog, which) -> {
            }));

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Метод який здійснює операцію видалення вибраного файлу чи каталогу
     */
    private void deleteFileOrDirectoryAction() {
        if (/*isPermission() &&*/ isExternalStorageWritable()) {
            if (item == null) {
                Toast.makeText(this, "Спочатку оберіть елемент для видалення", Toast.LENGTH_SHORT).show();
            } else {
                String fullDirectoryPath = currentDirectory.getAbsolutePath();
                File deleteFile = new File(fullDirectoryPath, item.getTitle());

                if (isStandardPublicDirectory(deleteFile)) {
                    Toast.makeText(this, "Неможливо видалити загальнодоступний каталог " + deleteFile.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog_Alert);

                    builder.setTitle("Видалення файла");
                    builder.setMessage("Ви впевнені, що хочете видалити файл [" + item.getTitle() + "]?");

                    builder.setPositiveButton("Видалити", (dialog, which) -> {

                        if (deleteFile.isFile()) {
                            if (deleteFile.delete()) {
                                Toast.makeText(this, "Файл [" + deleteFile.getName() + "] успішно видалено!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Помилка видалення файлу [" + deleteFile.getName() + "]", Toast.LENGTH_SHORT).show();
                            }
                        } else if (deleteFile.isDirectory()) {
                            if (recursiveDeleteCatalog(deleteFile)) {
                                Toast.makeText(this, "Каталог [" + deleteFile.getName() + "] успішно видалено!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Помилка видалення каталогу " + deleteFile.getName() + "]", Toast.LENGTH_SHORT).show();
                            }
                        }

                        createMainActivity(currentDirectory.getAbsolutePath());
                    });

                    builder.setNegativeButton("Відмінити", (dialog, which) -> {
                    });

                    AlertDialog dialog = builder.create();

                    dialog.show();
                }
            }
        }
    }

    /**
     * Метод для видалення файлів чи каталогів (рекурсивно).
     *
     * @param file - файл чи каталог.
     * @return [true] - якщо файл чи каталог видалено успішно.
     */
    private boolean recursiveDeleteCatalog(File file) {
        if (!file.exists() || isStandardPublicDirectory(file)) {
            return false;
        }

        File[] innerFilesList = file.listFiles();

        if (innerFilesList != null) {
            for (File currentFile : innerFilesList) {
                if (isExternalStorageWritable()) {
                    recursiveDeleteCatalog(currentFile);
                }
            }
        }

        return file.delete();
    }

    /**
     * Метод який здійснює операцію перейменування вибраного файлу чи каталогу.
     */
    private void renameAction() {
        if (/*isPermission() &&*/ isExternalStorageWritable()) {

            if (item == null) {
                Toast.makeText(this, "Спочатку оберіть елемент для зміни назви", Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Dialog_Alert);
                builder.setTitle("Зміна назви");

                LayoutInflater inflater = this.getLayoutInflater();
                final View view = inflater.inflate(R.layout.activity_dialog, null, false);
                EditText etFileNameDialog = view.findViewById(R.id.etFileNameDialog);
                etFileNameDialog.setText(item.getTitle());

                String oldFileName = item.getTitle();
                File oldFile = new File(currentDirectory.getPath(), oldFileName);

                if (oldFile.isDirectory() && isStandardPublicDirectory(oldFile)) {
                    Toast.makeText(this, "Неможливо перейменувати загальнодоступний каталог [" + oldFileName + "]!", Toast.LENGTH_SHORT).show();
                    return;

                } else {
                    builder.setView(view);

                    builder.setPositiveButton("Змінити назву", (dialog, which) -> {

                        String newFileName = etFileNameDialog.getText().toString();
                        File newFile = new File(currentDirectory.getPath(), newFileName);

                        boolean renameResult = oldFile.renameTo(newFile);

                        if (isExternalStorageWritable()) {
                            if (renameResult) {
                                if (oldFile.isFile()) {
                                    Toast.makeText(this, "Файл [" + oldFileName + "] упішно перейменовано на [" + newFileName + "]!",
                                            Toast.LENGTH_SHORT).show();
                                } else if (oldFile.isDirectory()) {
                                    Toast.makeText(this, "Каталог [" + oldFileName + "] упішно перейменовано на [" + newFileName + "]!",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (oldFile.isFile()) {
                                    Toast.makeText(this, "Помилка перейменування файлу [" + oldFileName + "] на [" + newFileName + "]!",
                                            Toast.LENGTH_SHORT).show();
                                } else if (oldFile.isDirectory()) {
                                    Toast.makeText(this, "Помилка перейменування каталогу [" + oldFileName + "] на [" + newFileName + "]!",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        createMainActivity(currentDirectory.getAbsolutePath());
                    });

                    builder.setNegativeButton("Відмінити", ((dialog, which) -> {
                    }));

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
    }

    /**
     * Метод який здійснює операцію копіювання вибраного файлу чи каталогу.
     */
    private void copyAction() {
        if (item == null) {
            Toast.makeText(this, "Спочатку оберіть елемент для копіювання", Toast.LENGTH_SHORT).show();
        } else {
            currentFile = new File(tvPath.getText().toString(), item.getTitle());

            if (currentFile.isFile()) {
                Toast.makeText(this, "Файл [" + currentFile.getName() + "] скопійовано!", Toast.LENGTH_SHORT).show();
            } else if (currentFile.isDirectory()) {
                Toast.makeText(this, "Каталог [" + currentFile.getName() + "] скопійовано!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Метод який здійснює операцію вставки вибраного файлу чи каталогу.
     */
    private void pasteAction() {
        if (/*isPermission() &&*/ isExternalStorageWritable()) {
            if (currentFile != null) {
                File target = new File(tvPath.getText().toString());
                copyFileOrDirectory(currentFile, target);

                createMainActivity(currentDirectory.getAbsolutePath());
                currentFile = null;
            } else {
                Toast.makeText(this, "Спочатку скопіюйте файл чи каталог", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Метод для створення копії каталога.
     *
     * @param root            - шлях до каталогу або файла, що копіюється.
     * @param targetDirectory - шлях до копії каталогу.
     */
    public void copyFileOrDirectory(File root, File targetDirectory) {
        if (root.isDirectory()) {
            recursiveCopy(root, targetDirectory);
            Toast.makeText(this, "Каталог [" + root.getName() + "] успішно скопійовано!", Toast.LENGTH_SHORT).show();
        } else if (root.isFile()) {
            File tar = checkIsCopy(new File(targetDirectory, root.getName()));
            copyFileContent(root, tar);
            Toast.makeText(this, "Файл [" + root.getName() + "] успішно скопійовано!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Метод для створення копії каталогу та при наявності  рекурсивне наповнення його вмістом
     * (внутршніми файлами та каталогами).
     *
     * @param root - цільовий каталог.
     * @param tar  - каталог призначення.
     */
    private void recursiveCopy(File root, File tar) {
        File[] files = root.listFiles();
        tar = checkIsCopy(new File(tar, root.getName()));
        tar.mkdir();

        for (File file : files) {
            String fileName = file.getName();
            File target = new File(tar, fileName);

            if (file.isFile()) {
                copyFileContent(file, target);
            } else if (file.isDirectory()) {
                target.mkdir();
                // Рекурсивне повторення
                recursiveCopy(file, target);
            }
        }
    }

    /**
     * Метод для копіювання вмісту (наповнення даними) файлу.
     *
     * @param root   - цільовий файл
     * @param target - файл призначення
     */
    private void copyFileContent(File root, File target) {
        FileOutputStream fos = null;
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(root);
            fos = new FileOutputStream(target);
            int length;
            byte[] buffer = new byte[1024];

            while ((length = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Метод для перевірки чи є файл стандартним "загальнодоступним каталогом".
     *
     * @param file - файл для перевірки;
     * @return [true] - якщо файл стандартний "загальнодоступний каталог", інакше - [false];
     */
    private boolean isStandardPublicDirectory(File file) {
        File music = new File(ROOT_DIRECTORY, Environment.DIRECTORY_MUSIC);
        File podcasts = new File(ROOT_DIRECTORY, Environment.DIRECTORY_PODCASTS);
        File ringtones = new File(ROOT_DIRECTORY, Environment.DIRECTORY_RINGTONES);
        File alarms = new File(ROOT_DIRECTORY, Environment.DIRECTORY_ALARMS);
        File notifications = new File(ROOT_DIRECTORY, Environment.DIRECTORY_NOTIFICATIONS);
        File pictures = new File(ROOT_DIRECTORY, Environment.DIRECTORY_PICTURES);
        File movies = new File(ROOT_DIRECTORY, Environment.DIRECTORY_MOVIES);
        File downloads = new File(ROOT_DIRECTORY, Environment.DIRECTORY_DOWNLOADS);
        File dcim = new File(ROOT_DIRECTORY, Environment.DIRECTORY_DCIM);
        File android = new File(ROOT_DIRECTORY, Environment.DIRECTORY_DOCUMENTS);

        if (file.equals(music) || file.equals(podcasts) || file.equals(ringtones) ||
                file.equals(alarms) || file.equals(notifications) || file.equals(pictures) ||
                file.equals(movies) || file.equals(downloads) || file.equals(dcim) ||
                file.equals(android)) {
            return true;
        }
        return false;
    }

    /**
     * Метод для перевірки чи є файл текстовим.
     *
     * @param file
     * @return [true] - так, файл текстовий, [false] - ні, файл іншого типу
     */
    private boolean isTextFile(File file) {
        String fileExtension = getFileExtension(file);

        if (fileExtension == "txt" || fileExtension == "doc" || fileExtension == "docx") {
            return true;
        }

        return false;
    }

    /**
     * Метод для отримання розширення файлу (наприклад, txt, doc, docx тощо).
     *
     * @param file - файл для оримання розширення
     * @return - розширення файлу
     */
    private static String getFileExtension(File file) {
        if (file.isFile()) {
            String fileName = file.getName();
            if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
                return fileName.substring(fileName.lastIndexOf(".") + 1);
            }
        }
        return null;
    }

    /**
     * Метод для перевірки чи існує в поточному каталозі файл чи директорія з такою ж назвою.
     *
     * @param checkFile - файл для перевірки на кипію назви.
     * @return File.
     */
    public File checkIsCopy(File checkFile) {
        File rootDirectory = new File(tvPath.getText().toString());
        File[] files = rootDirectory.listFiles();
        int count = 0;

        if (checkFile.isDirectory()) {
            String resultName = checkFile.getName();

            for (File file : files) {
                if (file.isDirectory() && file.getName().equals(resultName)) {
                    count++;
                    resultName = checkFile.getName() + "(" + count + ")";
                }
            }

            return new File(rootDirectory, resultName);
        } else if (checkFile.isFile()) {
            String resultName = checkFile.getName();

            for (File file : files) {
                if (checkFile.isFile() && file.getName().equals(resultName)) {
                    int index = checkFile.getName().lastIndexOf(".");
                    count++;
                    resultName = checkFile.getName().substring(0, index) + "(" + count + ")" + getFileExtension(checkFile);
                }
            }

            return new File(rootDirectory, resultName);
        } else {
            return checkFile;
        }
    }
}