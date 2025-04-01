package compImg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class Main {

	/**
	 * 이미지 용량 축소
	 * 
	 * @param inputImagePath
	 * @param outputImagePath
	 * @param quality
	 * @throws IOException
	 * @throws Exception
	 */
	public static void compressImageFile(String inputImagePath, String outputImagePath, float quality)
			throws IOException, Exception {
		System.out.println("원본 : " + inputImagePath);
		try {
			// 원본 이미지 파일을 읽습니다.
			File inputFile = new File(inputImagePath);
			BufferedImage image = ImageIO.read(inputFile);

			// 이미지가 성공적으로 로드되었는지 확인
			if (image == null) {
				System.out.println("이미지 로드 실패.");
				return;
			}
			
			if (!inputFile.getName().toLowerCase().endsWith(".jpg")) {
				System.out.println("***** JPG 파일이 아닙니다: " + inputFile.getName() + "********");
				return;
			}
			
			// 알파 채널 제거(색상 표현 32bit를 24bit로 변환)
			BufferedImage imageWithoutAlpha = stripAlphaChannel(image);

			// 이미지 파일 포맷을 설정합니다.
			File outputFile = new File(outputImagePath);
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

			// 압축 설정
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality); // 압축 품질을 0.0f(최대 압축) ~ 1.0f(원본 유지) 범위로 설정

			// 압축된 이미지 저장
			try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
				writer.setOutput(ios);
				writer.write(null, new javax.imageio.IIOImage(imageWithoutAlpha, null, null), param);
			}
			System.out.println("이미지가 압축되어 저장되었습니다.");
			System.out.println("수정본 : " + outputImagePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 폴더 전체 용량
	 * 
	 * @param folder
	 * @return
	 */
	public static long calculateFileSize(File folder) {
		long size = 0;

		// 폴더가 파일이 아니라면 폴더 내부의 파일 및 하위 폴더를 탐색
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				size += (file.isDirectory()) ? calculateFileSize(file) : file.length();
			}
		} else if (folder.isFile()) { // 파일 사이즈
			size = folder.length();
		}

		return size;
	}

	/**
	 * 바이트를 사람이 형식으로 변환
	 * 
	 * @param size
	 * @return
	 */
	public static String convertBytesToReadableSize(long size) {
		if (size <= 0)
			return "0 Byte";

		final String[] units = new String[] { "Byte", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
	}

	/**
	 * 파일건수
	 * 
	 * @param path
	 * @return
	 */
	public static int countFilesInFolder(String path) {
		File file = new File(path);

		if (file.isDirectory()) {
			return file.listFiles().length;
		} else if (file.isFile()) { // 파일이면 단건, defualt 1임.
			return 1;
		}
		return 0;

	}

	public static void main(String[] args) throws Exception {

		// Scanner 객체 생성
//		Scanner scanner = null;
		try {
			while (true) {
				JFileChooser fileChooser = new JFileChooser(); // 파일 선택 창
				fileChooser.setMultiSelectionEnabled(true); // 다중 파일 선택 활성화
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); // 파일과 디렉토리 선택 가능

				int result = fileChooser.showOpenDialog(null); // 파일 선택 다이얼로그 표시
				File[] selectedFiles = null;
				if (result == JFileChooser.APPROVE_OPTION) {
					selectedFiles = fileChooser.getSelectedFiles(); // 선택된 파일 및 디렉토리 배열 가져오기
					System.out.println("선택된 항목:");
					// summary
					long uploadSize = 0;
					long downloadSize = 0;
					int uploadCnt = 0;
					int downloadCnt = 0;
					for (File pathFile : selectedFiles) {
						if (pathFile.isDirectory()) {
							System.out.println("[디렉토리] " + pathFile.getAbsolutePath());
							String uploadDir = pathFile.getAbsolutePath(); // 디렉토리
							File uploadFile = new File(uploadDir);
							File[] files = uploadFile.listFiles(); // 디렉토리 안의 파일들
							String downloadDir = uploadDir + "_compress\\"; // 품질 변환된 파일 디렉토리
							alertBox(uploadDir, downloadDir); // 대상 디렉토리 및 변환 후 디렉토리 
							String execYn = confirmBox(); // 압축 실행 여부 디폴트 y
							if (!execYn.equals("y")) {
								return;
							}
							float vComp = inputCompressValue(); // 품질 압축 값
							
							// 변환 폴더 생성
							Path directoryPath = Paths.get(downloadDir);
							Files.createDirectories(directoryPath);

							// 압축 처리
							for (File file : files) {
								compressImageFile(file.getPath(), downloadDir + file.getName(), vComp);
							}
							uploadSize = calculateFileSize(uploadFile);
							downloadSize = calculateFileSize(new File(downloadDir));
							uploadCnt = countFilesInFolder(uploadDir);
							downloadCnt = countFilesInFolder(downloadDir);
							
							alertBox(downloadDir, uploadCnt, downloadCnt, vComp, uploadSize, downloadSize);
						} else {
							System.out.println("[파일] " + pathFile.getAbsolutePath());
							String uploadDir = pathFile.getAbsolutePath(); // 파일 경로
							File uploadFile = new File(uploadDir);
							String downloadDir = uploadDir + "_compress\\"; 
							// 디렉토리 경로만
							downloadDir = uploadDir.substring(0, uploadDir.lastIndexOf("\\") + 1);
							// 파일 이름만
							String fileName = uploadDir.substring(uploadDir.lastIndexOf("\\") + 1, uploadDir.lastIndexOf("."));
		
							String downloadFile = downloadDir + fileName + "_compress.jpg"; // 변환 파일명
							alertBox(uploadDir, downloadDir);
							String execYn = confirmBox();
							if (!execYn.equals("y")) {
								return;
							}
							float vComp = inputCompressValue();
							compressImageFile(uploadFile.getPath(), downloadFile, vComp);
							uploadSize = calculateFileSize(uploadFile);
							downloadSize = calculateFileSize(new File(downloadFile));
							uploadCnt = countFilesInFolder(uploadDir);
							downloadCnt = countFilesInFolder(downloadFile);
						}
					}
				} else {
					System.out.println("선택이 취소되었습니다.");
					return;
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * 알파 채널 삭제 jpg 이미지는 기본 24bit 설정이지만 alphachannel로 변경해서 32bit 까지 색 표현력을 올린다.
	 * 
	 * @param img
	 * @return
	 */
	public static BufferedImage stripAlphaChannel(BufferedImage img) {
		if (!img.getColorModel().hasAlpha()) {
			return img; // 알파 채널이 없으면 그대로 반환
		}

		BufferedImage target = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = target.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return target;
	}

	/**
	 * 경로 확인 - 파일과 폴더 확인
	 * 
	 * @param input
	 * @return
	 */
	public static String verifyPathType(File input) {
		// 파일 또는 디렉토리 존재 여부
		if (!input.exists()) {
			System.out.println("파일 및 디렉토리가 존재 하지 않습니다.: " + input);
			return "0";
		}

		if (input.isDirectory()) {
			System.out.println("이 경로는 디렉토리입니다.");
			// uploadFile.listFiles();
			File[] files = input.listFiles();
			if (null == files || files.length < 1) {
				System.out.println("파일이 존재 하지 않습니다.: " + input);
				return "0";
			}
			// 디렉토리
			return "1";
		} else if (input.isFile()) {
			System.out.println("이 경로는 파일입니다.");
			// 파일
			return "2";
		}
		return "0";
	}
	
	/**
	 * 확인 여부 입력 상자
	 * 
	 * @return
	 */
	public static String confirmBox() {
		 // 확인 대화 상자 표시
        int result = JOptionPane.showConfirmDialog(
            null,
            "압축을 진행하시겠습니까?", // 메시지
            "압축 확인 창", // 창 제목
            JOptionPane.YES_NO_OPTION // 버튼 옵션
        );

        // 결과 처리
        if (result == JOptionPane.YES_OPTION) {
            return "y";
        } else if (result == JOptionPane.NO_OPTION) {
            return "n";
        } else {
            return null;
        }
	}
	/**
	 * 압축 비율 입력 상자
	 * 
	 * @return
	 */
	public static float inputCompressValue() {
		// 기본값 설정
	    float defaultValue = 0.7f; // 기본 압축 품질 값

		// 입력 상자를 띄워 사용자 입력 받기
		String userInput = JOptionPane.showInputDialog(null, "이미지 압축 품질을 입력하세요 (0.1 ~ 1.0):" + defaultValue, "압축 품질 입력",
				JOptionPane.QUESTION_MESSAGE);
		try {
			float value = Float.parseFloat(userInput);
			// 사용자가 입력한 값 출력 및 값 범위 확인
			if (value >= 0.1f && value <= 1.0f) {
				return value;
			} else {
				JOptionPane.showMessageDialog(null, "값이 범위를 벗어났습니다. 0.1 ~ 1.0 사이 값을 입력해주세요.");
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "숫자를 입력해주세요.");
		}
		return defaultValue; // 기본값

	}
	/**
	 * alertBox 오버로딩 
	 * 
	 * @param input
	 */
	private static void alertBox(String input) { // 미사용
		String text = "입력한 값" + input + "이 유효하지 않습니다.";
		// 알럿 창 띄우기
		JOptionPane.showMessageDialog(null, text);
	}

	private static void alertBox(String uploadDir, String downloadDir) {
		// 압축 경로
		String text = "이미지 축소 대상 : " + uploadDir + "\n" 
					+ "용량 축소 완료된 파일의 폴더 위치 : " + downloadDir + "\n";
		// 알럿 창 띄우기
		JOptionPane.showMessageDialog(null, text);
	}
	
	private static void alertBox(String downloadDir, int uploadCnt, int downloadCnt, float fCompr, long uploadSize,
			long downloadSize) {
		// 압축 결과
		String text = "용량 축소가 완료 되었습니다. \n"
					+ "용량 축소 완료된 파일의 폴더 위치" + downloadDir + "\n" 
					+ "축소대상 파일수 : " + uploadCnt + " --> 축소후  파일수 : " + downloadCnt + "\n" 
					+ "압축비율 : 원본의 " + fCompr * 100 + " % 크기로 압축" + "\n" 
					+ "원본 크기 : " + convertBytesToReadableSize(uploadSize) + "\n" 
					+ "압축 크기 : " + convertBytesToReadableSize(downloadSize) + "\n";
		// 메세지창 
		JOptionPane.showMessageDialog(null, text);
	}

}
