package se.vgregion.dicom.scp

import org.dcm4che3.data.Attributes
import org.dcm4che3.data.Tag
import org.dcm4che3.data.VR
import org.dcm4che3.net.ApplicationEntity
import org.dcm4che3.net.Association
import org.dcm4che3.net.Connection
import org.dcm4che3.net.Device
import org.dcm4che3.net.PDVInputStream
import org.dcm4che3.net.TransferCapability
import org.dcm4che3.net.pdu.PresentationContext
import org.dcm4che3.net.service.BasicCEchoSCP
import org.dcm4che3.net.service.BasicCStoreSCP
import org.dcm4che3.net.service.DicomServiceRegistry

import com.typesafe.scalalogging.LazyLogging

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import se.vgregion.dicom.DicomProtocol.DatasetReceivedByScp

class Scp(val name: String, val aeTitle: String, val port: Int, notifyActor: ActorRef) extends LazyLogging {

  case class SopClass(sopClassName: String, sopClassUID: String, iodSpecification: String, included: Boolean)

  val sopClasses = Seq(
    SopClass("Media Storage Directory Storage", "1.2.840.10008.1.3.10", "Basic Directory IOD", false),
    SopClass("Computed Radiography Image Storage", "1.2.840.10008.5.1.4.1.1.1", "Computed Radiography Image IOD", true),
    SopClass("Digital X-Ray Image Storage - For Presentation", "1.2.840.10008.5.1.4.1.1.1.1", "Digital X-Ray Image IOD", true),
    SopClass("Digital X-Ray Image Storage - For Processing", "1.2.840.10008.5.1.4.1.1.1.1.1", "Digital X-Ray Image IOD", true),
    SopClass("Digital Mammography X-Ray Image Storage - For Presentation", "1.2.840.10008.5.1.4.1.1.1.2", "Digital Mammography X-Ray Image IOD", true),
    SopClass("Digital Mammography X-Ray Image Storage - For Processing", "1.2.840.10008.5.1.4.1.1.1.2.1", "Digital Mammography X-Ray Image IOD", true),
    SopClass("Digital Intra-Oral X-Ray Image Storage - For Presentation", "1.2.840.10008.5.1.4.1.1.1.3", "Digital Intra-Oral X-Ray Image IOD", true),
    SopClass("Digital Intra-Oral X-Ray Image Storage - For Processing", "1.2.840.10008.5.1.4.1.1.1.3.1", "Digital Intra-Oral X-Ray Image IOD", true),
    SopClass("CT Image Storage", "1.2.840.10008.5.1.4.1.1.2", "Computed Tomography Image IOD", true),
    SopClass("Enhanced CT Image Storage", "1.2.840.10008.5.1.4.1.1.2.1", "Enhanced CT Image IOD", true),
    SopClass("Legacy Converted Enhanced CT Image Storage", "1.2.840.10008.5.1.4.1.1.2.2", "Legacy Converted Enhanced CT Image IOD", true),
    SopClass("Ultrasound Multi-frame Image Storage", "1.2.840.10008.5.1.4.1.1.3.1", "Ultrasound Multi-frame Image IOD", true),
    SopClass("MR Image Storage", "1.2.840.10008.5.1.4.1.1.4", "Magnetic Resonance Image IOD", true),
    SopClass("Enhanced MR Image Storage", "1.2.840.10008.5.1.4.1.1.4.1", "Enhanced MR Image IOD", true),
    SopClass("MR Spectroscopy Storage", "1.2.840.10008.5.1.4.1.1.4.2", "MR Spectroscopy IOD", true),
    SopClass("Enhanced MR Color Image Storage", "1.2.840.10008.5.1.4.1.1.4.3", "Enhanced MR Color Image IOD", true),
    SopClass("Legacy Converted Enhanced MR Image Storage", "1.2.840.10008.5.1.4.1.1.4.4", "Legacy Converted Enhanced MR Image IOD", true),
    SopClass("Ultrasound Image Storage", "1.2.840.10008.5.1.4.1.1.6.1", "Ultrasound Image IOD", true),
    SopClass("Enhanced US Volume Storage", "1.2.840.10008.5.1.4.1.1.6.2", "Enhanced US Volume IOD", true),
    SopClass("Secondary Capture Image Storage", "1.2.840.10008.5.1.4.1.1.7", "Secondary Capture Image IOD", false),
    SopClass("Multi-frame Single Bit Secondary Capture Image Storage", "1.2.840.10008.5.1.4.1.1.7.1", "Multi-frame Single Bit Secondary Capture Image IOD", false),
    SopClass("Multi-frame Grayscale Byte Secondary Capture Image Storage", "1.2.840.10008.5.1.4.1.1.7.2", "Multi-frame Grayscale Byte Secondary Capture Image IOD", false),
    SopClass("Multi-frame Grayscale Word Secondary Capture Image Storage", "1.2.840.10008.5.1.4.1.1.7.3", "Multi-frame Grayscale Word Secondary Capture Image IOD", false),
    SopClass("Multi-frame True Color Secondary Capture Image Storage", "1.2.840.10008.5.1.4.1.1.7.4", "Multi-frame True Color Secondary Capture Image IOD", false),
    SopClass("12-lead ECG Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.1.1", "12-Lead Electrocardiogram IOD", false),
    SopClass("General ECG Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.1.2", "General Electrocardiogram IOD", false),
    SopClass("Ambulatory ECG Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.1.3", "Ambulatory Electrocardiogram IOD", false),
    SopClass("Hemodynamic Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.2.1", "Hemodynamic IOD", false),
    SopClass("Cardiac Electrophysiology Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.3.1", "Basic Cardiac Electrophysiology IOD", false),
    SopClass("Basic Voice Audio Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.4.1", "Basic Voice Audio IOD", false),
    SopClass("General Audio Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.4.2", "General Audio Waveform IOD", false),
    SopClass("Arterial Pulse Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.5.1", "Arterial Pulse Waveform IOD", false),
    SopClass("Respiratory Waveform Storage", "1.2.840.10008.5.1.4.1.1.9.6.1", "Respiratory Waveform IOD", false),
    SopClass("Grayscale Softcopy Presentation State Storage", "1.2.840.10008.5.1.4.1.1.11.1", "Grayscale Softcopy Presentation State IOD", false),
    SopClass("Color Softcopy Presentation State Storage", "1.2.840.10008.5.1.4.1.1.11.2", "Color Softcopy Presentation State IOD", false),
    SopClass("Pseudo-Color Softcopy Presentation State Storage", "1.2.840.10008.5.1.4.1.1.11.3", "Pseudo-color Softcopy Presentation State IOD", false),
    SopClass("Blending Softcopy Presentation State Storage", "1.2.840.10008.5.1.4.1.1.11.4", "Blending Softcopy Presentation State IOD", false),
    SopClass("XA/XRF Grayscale Softcopy Presentation State Storage", "1.2.840.10008.5.1.4.1.1.11.5", "XA/XRF Grayscale Softcopy Presentation State IOD", false),
    SopClass("X-Ray Angiographic Image Storage", "1.2.840.10008.5.1.4.1.1.12.1", "X-Ray Angiographic Image IOD", true),
    SopClass("Enhanced XA Image Storage", "1.2.840.10008.5.1.4.1.1.12.1.1", "Enhanced X-Ray Angiographic Image IOD", true),
    SopClass("X-Ray Radiofluoroscopic Image Storage", "1.2.840.10008.5.1.4.1.1.12.2", "X-Ray RF Image IOD", true),
    SopClass("Enhanced XRF Image Storage", "1.2.840.10008.5.1.4.1.1.12.2.1", "Enhanced X-Ray RF Image IOD", true),
    SopClass("X-Ray 3D Angiographic Image Storage", "1.2.840.10008.5.1.4.1.1.13.1.1", "X-Ray 3D Angiographic Image IOD", true),
    SopClass("X-Ray 3D Craniofacial Image Storage", "1.2.840.10008.5.1.4.1.1.13.1.2", "X-Ray 3D Craniofacial Image IOD", true),
    SopClass("Breast Tomosynthesis Image Storage", "1.2.840.10008.5.1.4.1.1.13.1.3", "Breast Tomosynthesis Image IOD", true),
    SopClass("Intravascular Optical Coherence Tomography Image Storage - For Presentation", "1.2.840.10008.5.1.4.1.1.14.1", "Intravascular OCT IOD", true),
    SopClass("Intravascular Optical Coherence Tomography Image Storage - For Processing", "1.2.840.10008.5.1.4.1.1.14.2", "Intravascular OCT IOD", true),
    SopClass("Nuclear Medicine Image Storage", "1.2.840.10008.5.1.4.1.1.20", "Nuclear Medicine Image IOD", true),
    SopClass("Raw Data Storage", "1.2.840.10008.5.1.4.1.1.66", "Raw Data IOD", true),
    SopClass("Spatial Registration Storage", "1.2.840.10008.5.1.4.1.1.66.1", "Spatial Registration IOD", false),
    SopClass("Spatial Fiducials Storage", "1.2.840.10008.5.1.4.1.1.66.2", "Spatial Fiducials IOD", false),
    SopClass("Deformable Spatial Registration Storage", "1.2.840.10008.5.1.4.1.1.66.3", "Deformable Spatial Registration IOD", false),
    SopClass("Segmentation Storage", "1.2.840.10008.5.1.4.1.1.66.4", "Segmentation IOD", false),
    SopClass("Surface Segmentation Storage", "1.2.840.10008.5.1.4.1.1.66.5", "Surface Segmentation IOD", false),
    SopClass("Real World Value Mapping Storage", "1.2.840.10008.5.1.4.1.1.67", "Real World Value Mapping IOD", false),
    SopClass("Surface Scan Mesh Storage", "1.2.840.10008.5.1.4.1.1.68.1", "Surface Scan Mesh IOD", false),
    SopClass("Surface Scan Point Cloud Storage", "1.2.840.10008.5.1.4.1.1.68.2", "Surface Scan Point Cloud IOD", false),
    SopClass("VL Endoscopic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.1", "VL Endoscopic Image IOD", true),
    SopClass("Video Endoscopic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.1.1", "Video Endoscopic Image IOD", true),
    SopClass("VL Microscopic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.2", "VL Microscopic Image IOD", true),
    SopClass("Video Microscopic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.2.1", "Video Microscopic Image IOD", true),
    SopClass("VL Slide-Coordinates Microscopic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.3", "VL Slide-coordinates Microscopic Image IOD", true),
    SopClass("VL Photographic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.4", "VL Photographic Image IOD", true),
    SopClass("Video Photographic Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.4.1", "Video Photographic Image IOD", true),
    SopClass("Ophthalmic Photography 8 Bit Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.5.1", "Ophthalmic Photography 8 Bit Image IOD", true),
    SopClass("Ophthalmic Photography 16 Bit Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.5.2", "Ophthalmic Photography 16 Bit Image IOD", true),
    SopClass("Stereometric Relationship Storage", "1.2.840.10008.5.1.4.1.1.77.1.5.3", "Stereometric Relationship IOD", false),
    SopClass("Ophthalmic Tomography Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.5.4", "Ophthalmic Tomography Image IOD", true),
    SopClass("VL Whole Slide Microscopy Image Storage", "1.2.840.10008.5.1.4.1.1.77.1.6", "VL Whole Slide Microscopy Image IOD", true),
    SopClass("Lensometry Measurements Storage", "1.2.840.10008.5.1.4.1.1.78.1", "Lensometry Measurements IOD", false),
    SopClass("Autorefraction Measurements Storage", "1.2.840.10008.5.1.4.1.1.78.2", "Autorefraction Measurements IOD", false),
    SopClass("Keratometry Measurements Storage", "1.2.840.10008.5.1.4.1.1.78.3", "Keratometry Measurements IOD", false),
    SopClass("Subjective Refraction Measurements Storage", "1.2.840.10008.5.1.4.1.1.78.4", "Subjective Refraction Measurements IOD", false),
    SopClass("Visual Acuity Storage Measurements Storage", "1.2.840.10008.5.1.4.1.1.78.5", "Visual Acuity Measurements IOD", false),
    SopClass("Spectacle Prescription Report Storage", "1.2.840.10008.5.1.4.1.1.78.6", "Spectacle Prescription Report IOD", false),
    SopClass("Ophthalmic Axial Measurements Storage", "1.2.840.10008.5.1.4.1.1.78.7", "Ophthalmic Axial Measurements IOD", false),
    SopClass("Intraocular Lens Calculations Storage", "1.2.840.10008.5.1.4.1.1.78.8", "Intraocular Lens Calculations IOD", false),
    SopClass("Macular Grid Thickness and Volume Report", "1.2.840.10008.5.1.4.1.1.79.1", "Macular Grid Thickness and Volume Report IOD", false),
    SopClass("Ophthalmic Visual Field Static Perimetry Measurements Storage", "1.2.840.10008.5.1.4.1.1.80.1", "Ophthalmic Visual Field Static Perimetry Measurements IOD", false),
    SopClass("Ophthalmic Thickness Map Storage", "1.2.840.10008.5.1.4.1.1.81.1", "Ophthalmic Thickness Map IOD", false),
    SopClass("Corneal Topography Map Storage", "1.2.840.10008.5.1.4.1.1.82.1", "Corneal Topography Map IOD", false),
    SopClass("Basic Text SR", "1.2.840.10008.5.1.4.1.1.88.11", "Basic Text SR IOD", false),
    SopClass("Enhanced SR", "1.2.840.10008.5.1.4.1.1.88.22", "Enhanced SR IOD", false),
    SopClass("Comprehensive SR", "1.2.840.10008.5.1.4.1.1.88.33", "Comprehensive SR IOD", false),
    SopClass("Comprehensive 3D SR", "1.2.840.10008.5.1.4.1.1.88.34", "Comprehensive 3D SR IOD", false),
    SopClass("Procedure Log", "1.2.840.10008.5.1.4.1.1.88.40", "Procedure Log IOD", false),
    SopClass("Mammography CAD SR", "1.2.840.10008.5.1.4.1.1.88.50", "Mammography CAD SR IOD", false),
    SopClass("Key Object Selection Document", "1.2.840.10008.5.1.4.1.1.88.59", "Key Object Selection Document IOD", false),
    SopClass("Chest CAD SR", "1.2.840.10008.5.1.4.1.1.88.65", "Chest CAD SR IOD", false),
    SopClass("X-Ray Radiation Dose SR", "1.2.840.10008.5.1.4.1.1.88.67", "X-Ray Radiation Dose SR IOD", false),
    SopClass("Colon CAD SR", "1.2.840.10008.5.1.4.1.1.88.69", "Colon CAD SR IOD", false),
    SopClass("Implantation Plan SR Document Storage", "1.2.840.10008.5.1.4.1.1.88.70", "Implantation Plan SR Document IOD", false),
    SopClass("Encapsulated PDF Storage", "1.2.840.10008.5.1.4.1.1.104.1", "Encapsulated PDF IOD", false),
    SopClass("Encapsulated CDA Storage", "1.2.840.10008.5.1.4.1.1.104.2", "Encapsulated CDA IOD", false),
    SopClass("Positron Emission Tomography Image Storage", "1.2.840.10008.5.1.4.1.1.128", "Positron Emission Tomography Image IOD", true),
    SopClass("Enhanced PET Image Storage", "1.2.840.10008.5.1.4.1.1.130", "Enhanced PET Image IOD", true),
    SopClass("Legacy Converted Enhanced PET Image Storage", "1.2.840.10008.5.1.4.1.1.128.1", "Legacy Converted Enhanced PET Image IOD", true),
    SopClass("Basic Structured Display Storage", "1.2.840.10008.5.1.4.1.1.131", "Basic Structured Display IOD", false),
    SopClass("RT Image Storage", "1.2.840.10008.5.1.4.1.1.481.1", "RT Image IOD", true),
    SopClass("RT Dose Storage", "1.2.840.10008.5.1.4.1.1.481.2", "RT Dose IOD", false),
    SopClass("RT Structure Set Storage", "1.2.840.10008.5.1.4.1.1.481.3", "RT Structure Set IOD", false),
    SopClass("RT Beams Treatment Record Storage", "1.2.840.10008.5.1.4.1.1.481.4", "RT Beams Treatment Record IOD", false),
    SopClass("RT Plan Storage", "1.2.840.10008.5.1.4.1.1.481.5", "RT Plan IOD", false),
    SopClass("RT Brachy Treatment Record Storage", "1.2.840.10008.5.1.4.1.1.481.6", "RT Brachy Treatment Record IOD", false),
    SopClass("RT Treatment Summary Record Storage", "1.2.840.10008.5.1.4.1.1.481.7", "RT Treatment Summary Record IOD", false),
    SopClass("RT Ion Plan Storage", "1.2.840.10008.5.1.4.1.1.481.8", "RT Ion Plan IOD", false),
    SopClass("RT Ion Beams Treatment Record Storage", "1.2.840.10008.5.1.4.1.1.481.9", "RT Ion Beams Treatment Record IOD", false),
    SopClass("RT Beams Delivery Instruction Storage", "1.2.840.10008.5.1.4.34.7", "RT Beams Delivery Instruction IOD", false),
    SopClass("Hanging Protocol Storage", "1.2.840.10008.5.1.4.38.1", "Hanging Protocol IOD", false),
    SopClass("Color Palette Storage", "1.2.840.10008.5.1.4.39.1", "Color Palette IOD", false),
    SopClass("Generic Implant Template Storage", "1.2.840.10008.5.1.4.43.1", "Generic Implant Template IOD", false),
    SopClass("Implant Assembly Template Storage", "1.2.840.10008.5.1.4.44.1", "Implant Assembly Template IOD", false),
    SopClass("Implant Template Group Storage", "1.2.840.10008.5.1.4.45.1", "Implant Template Group IOD", false))

  val device = new Device("storescp")
  private val ae = new ApplicationEntity(aeTitle)
  private val conn = new Connection(name, null, port)

  private val cstoreSCP = new BasicCStoreSCP("*") {

    override protected def store(as: Association, pc: PresentationContext, rq: Attributes, data: PDVInputStream, rsp: Attributes): Unit = {
      rsp.setInt(Tag.Status, VR.US, 0)

      val cuid = rq.getString(Tag.AffectedSOPClassUID)
      val iuid = rq.getString(Tag.AffectedSOPInstanceUID)
      val tsuid = pc.getTransferSyntax()
      val dataset = data.readDataset(tsuid)
      notifyActor ! DatasetReceivedByScp(dataset)
    }

  }

  device.setDimseRQHandler(createServiceRegistry());
  device.addConnection(conn);
  device.addApplicationEntity(ae);

  ae.setAssociationAcceptor(true);
  ae.addConnection(conn);
  
  sopClasses.filter(_.included).foreach(sopClass =>
    ae.addTransferCapability(new TransferCapability(sopClass.sopClassUID, "*", TransferCapability.Role.SCP, "*")))

  private def createServiceRegistry(): DicomServiceRegistry = {
    val serviceRegistry = new DicomServiceRegistry()
    serviceRegistry.addDicomService(new BasicCEchoSCP())
    serviceRegistry.addDicomService(cstoreSCP)
    serviceRegistry
  }

}